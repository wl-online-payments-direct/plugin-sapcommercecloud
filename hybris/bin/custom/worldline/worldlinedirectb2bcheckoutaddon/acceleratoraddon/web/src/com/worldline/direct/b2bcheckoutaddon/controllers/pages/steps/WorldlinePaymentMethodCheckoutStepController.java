/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.b2bcheckoutaddon.controllers.pages.steps;


import com.onlinepayments.domain.CreateHostedTokenizationResponse;
import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.b2bcheckoutaddon.constants.WorldlineCheckoutConstants;
import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.b2bcheckoutaddon.forms.WorldlineAddressForm;
import com.worldline.direct.b2bcheckoutaddon.forms.WorldlinePaymentDetailsForm;
import com.worldline.direct.b2bcheckoutaddon.forms.validation.WorldlinePaymentDetailsValidator;
import com.worldline.direct.b2bcheckoutaddon.utils.WorldlineAddressDataUtil;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateQuoteCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.enums.CountryType;
import de.hybris.platform.util.Config;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_APPLEPAY;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.root)
public class    WorldlinePaymentMethodCheckoutStepController extends AbstractCheckoutStepController {
    protected static final Map<String, String> CYBERSOURCE_SOP_CARD_TYPES = new HashMap<>();
    private static final String PAYMENT_METHOD = "payment-method";
    private static final String CART_DATA_ATTR = "cartData";

    private static final Logger LOGGER = Logger.getLogger(WorldlinePaymentMethodCheckoutStepController.class);

    static {
        // Map hybris card type to Cybersource SOP credit card
        CYBERSOURCE_SOP_CARD_TYPES.put("visa", "001");
        CYBERSOURCE_SOP_CARD_TYPES.put("master", "002");
        CYBERSOURCE_SOP_CARD_TYPES.put("amex", "003");
        CYBERSOURCE_SOP_CARD_TYPES.put("diners", "005");
        CYBERSOURCE_SOP_CARD_TYPES.put("maestro", "024");
    }

    @Resource(name = "worldlineDefaultAddressDataUtil")
    private WorldlineAddressDataUtil addressDataUtil;
    @Resource(name = "userFacade")
    private UserFacade userFacade;
    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;
    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;
    @Resource(name = "worldlinePaymentDetailsValidator")
    private WorldlinePaymentDetailsValidator worldlinePaymentDetailsValidator;
    @Resource(name = "worldlinePaymentProductFilterStrategyFactory")
    private WorldlinePaymentProductFilterStrategyFactory worldlinePaymentProductFilterStrategyFactory;

    @Resource(name = "worldlineConfigurationService")
    private WorldlineConfigurationService worldlineConfigurationService;

    @ModelAttribute("billingCountries")
    public Collection<CountryData> getBillingCountries() {
        return getCheckoutFacade().getCountries(CountryType.BILLING);
    }

    @ModelAttribute("applePayId")
    int getApplePayId() {
        return PAYMENT_METHOD_APPLEPAY;
    }

    @ModelAttribute("idealID")
    int getIdealId() {
        return PAYMENT_METHOD_IDEAL;
    }

    @Override
    @RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.select, method = GET)
    @RequireHardLogIn
    @PreValidateQuoteCheckoutStep
    @PreValidateCheckoutStep(checkoutStep = PAYMENT_METHOD)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException {
        getCheckoutFacade().setDeliveryModeIfAvailable();
        setupAddPaymentPage(model);
        model.addAttribute("worldlinePaymentDetailsForm", new WorldlinePaymentDetailsForm());
        final List<PaymentProduct> availablePaymentMethods = worldlinePaymentProductFilterStrategyFactory.filter(worldlineCheckoutFacade.getAvailablePaymentMethods(), WorldlinePaymentProductFilterEnum.ACTIVE_PAYMENTS).get();
        List<PaymentProduct> filteredPaymentProducts = worldlinePaymentProductFilterStrategyFactory.filter(availablePaymentMethods, WorldlinePaymentProductFilterEnum.CHECKOUT_TYPE, WorldlinePaymentProductFilterEnum.GROUP_CARDS).get();

        model.addAttribute("paymentProducts", filteredPaymentProducts);
        model.addAttribute("idealIssuers", worldlineCheckoutFacade.getIdealIssuers(availablePaymentMethods));
        model.addAttribute("applySurcharge",(BooleanUtils.isTrue(worldlineConfigurationService.getCurrentWorldlineConfiguration().isApplySurcharge())));
        model.addAttribute("isCardPaymentMethodExisting", worldlineCheckoutFacade.checkForCardPaymentMethods(filteredPaymentProducts));

        if (WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(worldlineCheckoutFacade.getWorldlineCheckoutType())) {
            final CreateHostedTokenizationResponse hostedTokenization = worldlineCheckoutFacade.createHostedTokenization();
            model.addAttribute("hostedTokenization", hostedTokenization);
        }
        model.addAttribute("savedPaymentInfos", worldlineUserFacade.getWorldlinePaymentInfosForPaymentProducts(availablePaymentMethods, Boolean.TRUE));

        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        model.addAttribute(CART_DATA_ATTR, cartData);

        return WorldlineCheckoutConstants.Views.Pages.MultiStepCheckout.worldlinePaymentMethod;
    }

    @RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.select, method = RequestMethod.POST)
    @RequireHardLogIn
    public String add(final Model model, @Valid final WorldlinePaymentDetailsForm worldlinePaymentDetailsForm, final RedirectAttributes redirectAttributes, final BindingResult bindingResult)
            throws CMSItemNotFoundException {
        worldlinePaymentDetailsValidator.validate(worldlinePaymentDetailsForm, bindingResult);
        setupAddPaymentPage(model);
        if (bindingResult.hasErrors()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.paymentethod.formentry.invalid");
            return enterStep(model, redirectAttributes);
        }

        final WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();
        try {
            worldlineCheckoutFacade.fillWorldlinePaymentInfoData(worldlinePaymentInfoData,
                    worldlinePaymentDetailsForm.getSavedCardCode(),
                    worldlinePaymentDetailsForm.getPaymentProductId(),
                    worldlinePaymentDetailsForm.getIssuerId(),
                    worldlinePaymentDetailsForm.getHostedTokenizationId());
        } catch (WorldlineNonValidPaymentProductException e) {
            GlobalMessages.addErrorMessage(model, "checkout.error.paymentproduct.invalid");
            return enterStep(model, redirectAttributes);
        }
        final AddressData addressData;
        if (Boolean.TRUE.equals(worldlinePaymentDetailsForm.isUseDeliveryAddress())) {
            addressData = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            if (addressData == null) {
                GlobalMessages.addErrorMessage(model,
                        "checkout.multi.paymentMethod.createSubscription.billingAddress.noneSelectedMsg");
                return enterStep(model, redirectAttributes);
            }
            addressData.setBillingAddress(Boolean.TRUE);
        } else {
            final WorldlineAddressForm addressForm = worldlinePaymentDetailsForm.getBillingAddress();
            addressData = addressDataUtil.convertToAddressData(addressForm);
            addressData.setBillingAddress(Boolean.TRUE);
        }

        getAddressVerificationFacade().verifyAddressData(addressData);
        worldlinePaymentInfoData.setBillingAddress(addressData);

        worldlineCheckoutFacade.handlePaymentInfo(worldlinePaymentInfoData);

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

        final WorldlinePaymentDetailsForm worldlinePaymentDetailsForm = new WorldlinePaymentDetailsForm();
        final WorldlineAddressForm addressForm = new WorldlineAddressForm();
        model.addAttribute("worldlinePaymentDetailsForm", worldlinePaymentDetailsForm);
        if (useDeliveryAddress) {
            final AddressData deliveryAddress = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            addressDataUtil.convert(deliveryAddress, addressForm);
        }
        worldlinePaymentDetailsForm.setBillingAddress(addressForm);
        return WorldlineCheckoutConstants.Views.Fragments.Checkout.BillingAddressForm;
    }

    protected void setupAddPaymentPage(final Model model) throws CMSItemNotFoundException {
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

    @ModelAttribute("hostedTokenizationJs")
    String getHostedTokenizationJs() {
        return Config.getParameter("worldline.hosted.tokenization.js");
    }


    @RequestMapping(value = "/back", method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    public String back(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().previousStep();
    }

    @RequestMapping(value = "/next", method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    public String next(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().nextStep();
    }

    protected CheckoutStep getCheckoutStep() {
        return getCheckoutStep(PAYMENT_METHOD);
    }

}
