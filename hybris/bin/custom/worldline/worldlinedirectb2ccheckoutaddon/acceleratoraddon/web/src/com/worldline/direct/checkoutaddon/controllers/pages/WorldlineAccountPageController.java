package com.worldline.direct.checkoutaddon.controllers.pages;

import javax.annotation.Resource;

import com.worldline.direct.checkoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.checkoutaddon.controllers.utils.WorldlineAddressDataUtil;
import com.worldline.direct.checkoutaddon.forms.WorldlineAddressForm;
import com.worldline.direct.constants.WorldlineCheckoutConstants;
import com.worldline.direct.facade.WorldlineUserFacade;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.ResourceBreadcrumbBuilder;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractSearchPageController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.customer.CustomerFacade;

import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.commerceservices.enums.CountryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;

@Controller
@RequestMapping({WorldlineWebConstants.URL.Account.root})
public class WorldlineAccountPageController extends AbstractSearchPageController {

    private static final String ADD_EDIT_ADDRESS_CMS_PAGE = "add-edit-address";
    private static final String PAYMENT_DETAILS_CMS_PAGE = "payment-details";
    private static final String COUNTRY_ATTR = "country";
    private static final String REGIONS_ATTR = "regions";
    private static final String ADDRESS_DATA_ATTR = "addressData";
    private static final String ADDRESS_FORM_ATTR = "addressForm";

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineAccountPageController.class);

    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;
    @Resource(name = "customerFacade")
    private CustomerFacade customerFacade;
    @Resource(name = "accountBreadcrumbBuilder")
    private ResourceBreadcrumbBuilder accountBreadcrumbBuilder;
    @Resource(name = "i18NFacade")
    private I18NFacade i18NFacade;
    @Resource(name = "acceleratorCheckoutFacade")
    private CheckoutFacade checkoutFacade;
    @Resource(name = "worldlineDefaultAddressDataUtil")
    private WorldlineAddressDataUtil addressDataUtil;
    @Resource(name = "userFacade")
    private UserFacade userFacade;
    @ModelAttribute("titles")
    public Collection<TitleData> getTitles()
    {
        return userFacade.getTitles();
    }

    @RequestMapping(
            value = {"/payment-details"},
            method = {RequestMethod.GET}
    )
    @RequireHardLogIn
    public String paymentDetails(Model model) throws CMSItemNotFoundException {
        model.addAttribute("customerData", customerFacade.getCurrentCustomer());
        model.addAttribute("worldlinePaymentInfoData", worldlineUserFacade.getWorldlinePaymentInfos(Boolean.TRUE));
        storeCmsPageInModel(model, getContentPageForLabelOrId(PAYMENT_DETAILS_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
        model.addAttribute("breadcrumbs", accountBreadcrumbBuilder.getBreadcrumbs("text.account.paymentDetails"));
        model.addAttribute("metaRobots", "noindex,nofollow");
        return this.getViewForPage(model);
    }


    @RequestMapping(
            value = {"/remove-payment-detail"},
            method = {RequestMethod.POST}
    )
    @RequireHardLogIn
    public String removePaymentMethod(@RequestParam("paymentInfoId") String paymentMethodId, RedirectAttributes redirectAttributes) throws CMSItemNotFoundException {
        try {
            worldlineUserFacade.deleteSavedWorldlinePaymentInfo(paymentMethodId);
            GlobalMessages.addFlashMessage(redirectAttributes, "accConfMsgs", "text.account.profile.paymentCart.removed");
        } catch (Exception exception) {
            GlobalMessages.addFlashMessage(redirectAttributes, "accErrorMsgs", "text.account.profile.paymentCart.remove.fail");
        }
        return "redirect:" + WorldlineWebConstants.URL.Account.PaymentDetails.root;
    }


    @RequestMapping(value = "/worldlineaddressform", method = RequestMethod.GET)
    public String getCountryAddressForm(@RequestParam("addressCode") final String addressCode,
          @RequestParam("countryIsoCode") final String countryIsoCode, final Model model)
    {
        model.addAttribute("supportedCountries", getCountries());
        populateModelRegionAndCountry(model, countryIsoCode);

        final WorldlineAddressForm addressForm = new WorldlineAddressForm();
        model.addAttribute(ADDRESS_FORM_ATTR, addressForm);
        for (final AddressData addressData : userFacade.getAddressBook())
        {
            if (addressData.getId() != null && addressData.getId().equals(addressCode)
                  && countryIsoCode.equals(addressData.getCountry().getIsocode()))
            {
                model.addAttribute(ADDRESS_DATA_ATTR, addressData);
                addressDataUtil.convert(addressData, addressForm);
                break;
            }
        }
        return WorldlineCheckoutConstants.Views.Fragments.Checkout.CountryAddressForm;
    }

    protected void populateModelRegionAndCountry(final Model model, final String countryIsoCode)
    {
        model.addAttribute(REGIONS_ATTR, i18NFacade.getRegionsForCountryIso(countryIsoCode));
        model.addAttribute(COUNTRY_ATTR, countryIsoCode);
    }

    @ModelAttribute("countries")
    public Collection<CountryData> getCountries()
    {
        return checkoutFacade.getCountries(CountryType.SHIPPING);
    }

}
