package com.ingenico.ogone.direct.controllers.pages.steps;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.List;

import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.AddressForm;
import de.hybris.platform.acceleratorstorefrontcommons.util.AddressDataUtil;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.enums.CountryType;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.constants.Ingenicoogonedirectb2ccheckoutaddonConstants;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.forms.IngenicoPaymentDetailsForm;
import com.ingenico.ogone.direct.forms.validation.IngenicoPaymentDetailsValidator;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

@Controller
@RequestMapping(value = "/checkout/multi/ingenico")
public class SelectIngenicoPaymentMethodCheckoutStepController extends AbstractCheckoutStepController {
    private static final Logger LOGGER = Logger.getLogger(SelectIngenicoPaymentMethodCheckoutStepController.class);

    private static final String CART_DATA_ATTR = "cartData";

    protected static final String PAYMENT_METHOD_STEP_NAME = "choose-payment-method";

    @Resource(name = "addressDataUtil")
    private AddressDataUtil addressDataUtil;

    @Resource(name = "userFacade")
    private UserFacade userFacade;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "ingenicoPaymentDetailsValidator")
    private IngenicoPaymentDetailsValidator ingenicoPaymentDetailsValidator;

    protected UserFacade getUserFacade() {
        return userFacade;
    }


    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * {@inheritDoc}
     */
    @Override
    @RequestMapping(value = "/select-payment-method", method = GET)
    @RequireHardLogIn
    @PreValidateCheckoutStep(checkoutStep = PAYMENT_METHOD_STEP_NAME)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException {
        getCheckoutFacade().setDeliveryModeIfAvailable();
        setupSelectPaymentPage(model);

        model.addAttribute("ingenicoPaymentDetailsForm", new IngenicoPaymentDetailsForm());
        final List<PaymentProduct> availablePaymentMethods = ingenicoCheckoutFacade.getAvailablePaymentMethods();
        model.addAttribute("paymentProducts", availablePaymentMethods);
//        model.addAttribute("idealIssuers",  ingenicoCheckoutFacade.getIdealIssuers(availablePaymentMethods));

        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        model.addAttribute(CART_DATA_ATTR, cartData);

        return Ingenicoogonedirectb2ccheckoutaddonConstants.Views.Pages.MultiStepCheckout.SelectPaymentMethod;
    }

    @RequestMapping(value = "/select-payment-method", method = RequestMethod.POST)
    @RequireHardLogIn
    public String add(final Model model, @Valid final IngenicoPaymentDetailsForm ingenicoPaymentDetailsForm, final RedirectAttributes redirectAttributes, final BindingResult bindingResult)
            throws CMSItemNotFoundException {
        ingenicoPaymentDetailsValidator.validate(ingenicoPaymentDetailsForm, bindingResult);
        setupSelectPaymentPage(model);

        if (bindingResult.hasErrors()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.paymentethod.formentry.invalid");
            return enterStep(model, redirectAttributes);
        }

        final IngenicoPaymentInfoData ingenicoPaymentInfoData = new IngenicoPaymentInfoData();
        ingenicoCheckoutFacade.fillIngenicoPaymentInfoData(ingenicoPaymentInfoData, ingenicoPaymentDetailsForm.getPaymentProductId());


        final AddressData addressData;
        if (Boolean.TRUE.equals(ingenicoPaymentDetailsForm.isUseDeliveryAddress())) {
            addressData = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            if (addressData == null) {
                GlobalMessages.addErrorMessage(model,
                        "checkout.multi.paymentMethod.createSubscription.billingAddress.noneSelectedMsg");
                return enterStep(model, redirectAttributes);
            }
            addressData.setBillingAddress(Boolean.TRUE);
        } else {
            final AddressForm addressForm = ingenicoPaymentDetailsForm.getBillingAddress();
            addressData = addressDataUtil.convertToAddressData(addressForm);
            addressData.setBillingAddress(Boolean.TRUE);
        }

        getAddressVerificationFacade().verifyAddressData(addressData);
        ingenicoPaymentInfoData.setBillingAddress(addressData);

        ingenicoCheckoutFacade.handlePaymentInfo(ingenicoPaymentInfoData);

        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        model.addAttribute(CART_DATA_ATTR, cartData);

        setCheckoutStepLinksForModel(model, getCheckoutStep());

        return getCheckoutStep().nextStep();
    }


    @RequestMapping(value = "/billingaddressform", method = RequestMethod.GET)
    @RequireHardLogIn
    public String getCountryAddressForm(@RequestParam("countryIsoCode") final String countryIsoCode,
                                        @RequestParam("useDeliveryAddress") final boolean useDeliveryAddress, final Model model) {
        model.addAttribute("supportedCountries", getCheckoutFacade().getCountries(CountryType.BILLING));
        model.addAttribute("regions", getI18NFacade().getRegionsForCountryIso(countryIsoCode));
        model.addAttribute("country", countryIsoCode);

        final IngenicoPaymentDetailsForm ingenicoPaymentDetailsForm = new IngenicoPaymentDetailsForm();
        final AddressForm addressForm = new AddressForm();
        model.addAttribute("ingenicoPaymentDetailsForm", ingenicoPaymentDetailsForm);
        if (useDeliveryAddress) {
            final AddressData deliveryAddress = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            addressDataUtil.convert(deliveryAddress, addressForm);
        }
        ingenicoPaymentDetailsForm.setBillingAddress(addressForm);
        return Ingenicoogonedirectb2ccheckoutaddonConstants.Views.Fragments.Checkout.BillingAddressForm;
    }

    protected void setupSelectPaymentPage(final Model model) throws CMSItemNotFoundException {
        model.addAttribute("metaRobots", "noindex,nofollow");
        model.addAttribute("hasNoPaymentInfo", Boolean.valueOf(getCheckoutFlowFacade().hasNoPaymentInfo()));
        prepareDataForPage(model);
        model.addAttribute(WebConstants.BREADCRUMBS_KEY,
                getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.paymentMethod.breadcrumb"));
        final ContentPageModel contentPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
        storeCmsPageInModel(model, contentPage);
        setUpMetaDataForContentPage(model, contentPage);
        setCheckoutStepLinksForModel(model, getCheckoutStep());
    }


    /**
     * {@inheritDoc}
     */
    @RequestMapping(value = "/back", method = GET)
    @RequireHardLogIn
    @Override
    public String back(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().previousStep();
    }

    /**
     * {@inheritDoc}
     */
    @RequestMapping(value = "/next", method = GET)
    @RequireHardLogIn
    @Override
    public String next(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().nextStep();
    }

    /**
     * {@inheritDoc}
     */
    protected CheckoutStep getCheckoutStep() {
        return getCheckoutStep(PAYMENT_METHOD_STEP_NAME);
    }
}
