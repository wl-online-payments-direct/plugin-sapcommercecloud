/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.b2bcheckoutaddon.controllers.pages.steps;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.b2bcheckoutaddon.constants.WorldlineCheckoutConstants;
import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.b2bcheckoutaddon.forms.WorldlinePlaceOrderForm;
import com.worldline.direct.b2bcheckoutaddon.utils.WorldlinePlaceOrderUtils;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.acceleratorservices.enums.CheckoutPciOptionEnum;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateQuoteCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.exception.EntityValidationException;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.payment.AdapterException;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Controller
@RequestMapping(value = WorldlineWebConstants.URL.Checkout.Summary.root)
public class WorldlineSummaryCheckoutStepController extends AbstractCheckoutStepController {
    private static final Logger LOG = Logger.getLogger(WorldlineSummaryCheckoutStepController.class);
    private static final String SUMMARY = "summary";
    private static final String REDIRECT_URL_REPLENISHMENT_CONFIRMATION = REDIRECT_PREFIX
            + "/checkout/replenishment/confirmation/";
    private static final String TEXT_STORE_DATEFORMAT_KEY = "text.store.dateformat";
    private static final String DEFAULT_DATEFORMAT = "MM/dd/yyyy";
    private static final String ACCEPT = "accept";
    private static final String USER_AGENT = "user-agent";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";


    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "siteBaseUrlResolutionService")
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Resource(name = "worldLineExtendedB2BCheckoutFacade")
    private CheckoutFacade extendedCheckoutFacade;

    @Resource(name = "worldlinePlaceOrderUtils")
    private WorldlinePlaceOrderUtils worldlinePlaceOrderUtils;

    @RequestMapping(value = "/view", method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    @PreValidateQuoteCheckoutStep
    @PreValidateCheckoutStep(checkoutStep = SUMMARY)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes)
            throws CMSItemNotFoundException {
        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        if (cartData.getEntries() != null && !cartData.getEntries().isEmpty()) {
            for (final OrderEntryData entry : cartData.getEntries()) {
                final String productCode = entry.getProduct().getCode();
                final ProductData product = getProductFacade().getProductForCodeAndOptions(productCode, Arrays.asList(
                        ProductOption.BASIC, ProductOption.PRICE, ProductOption.VARIANT_MATRIX_BASE, ProductOption.PRICE_RANGE));
                entry.setProduct(product);
            }
        }
        WorldlinePaymentInfoData worldlinePaymentInfo = cartData.getWorldlinePaymentInfo();
        if (cartData.getPaymentType() != null && CheckoutPaymentType.CARD.getCode().equals(cartData.getPaymentType().getCode())) {
            PaymentProduct paymentProduct;
            if (worldlinePaymentInfo.getId() == null) {
                GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                        "checkout.multi.paymentDetails.notprovided");
                return back(redirectAttributes);
            }
            paymentProduct = worldlineCheckoutFacade.getPaymentMethodById(worldlinePaymentInfo.getId());
            // TODO TRY CATCH  "not found paymentproduct worldline exception" instead of == null
            if (paymentProduct == null) {
                GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                        "checkout.multi.paymentDetails.notprovided");
                return back(redirectAttributes);
            }
        }

        model.addAttribute("cartData", cartData);
        model.addAttribute("allItems", cartData.getEntries());
        model.addAttribute("deliveryAddress", cartData.getDeliveryAddress());
        model.addAttribute("deliveryMode", cartData.getDeliveryMode());
        model.addAttribute("paymentInfo", cartData.getPaymentInfo());
        // TODO:Make configuration backoffice driven than hardcoding in controllers
        model.addAttribute("nDays", getNumberRange(1, 30));
        model.addAttribute("nthDayOfMonth", getNumberRange(1, 31));
        model.addAttribute("nthWeek", getNumberRange(1, 12));
        model.addAttribute("daysOfWeek", getB2BCheckoutFacade().getDaysOfWeekForReplenishmentCheckoutSummary());
        if (worldlinePaymentInfo != null) {
            model.addAttribute("showReplenishment", WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT.equals(worldlinePaymentInfo.getWorldlineCheckoutType()) && WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(worldlinePaymentInfo));
        } else {
            model.addAttribute("showReplenishment", true);
        }

        // Only request the security code if the SubscriptionPciOption is set to Default.
        final boolean requestSecurityCode = (CheckoutPciOptionEnum.DEFAULT
                .equals(getCheckoutFlowFacade().getSubscriptionPciOption()));
        model.addAttribute("requestSecurityCode", Boolean.valueOf(requestSecurityCode));

        if (!model.containsAttribute("worldlinePlaceOrderForm")) {
            final WorldlinePlaceOrderForm worldlinePlaceOrderForm = new WorldlinePlaceOrderForm();
            // TODO: Make setting of default recurrence enum value backoffice driven rather hard coding in controller
            worldlinePlaceOrderForm.setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum.MONTHLY);
            worldlinePlaceOrderForm.setnDays("14");
            final List<DayOfWeek> daysOfWeek = new ArrayList();
            daysOfWeek.add(DayOfWeek.MONDAY);
            worldlinePlaceOrderForm.setnDaysOfWeek(daysOfWeek);
            model.addAttribute("worldlinePlaceOrderForm", worldlinePlaceOrderForm);
        }

        final ContentPageModel multiCheckoutSummaryPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
        storeCmsPageInModel(model, multiCheckoutSummaryPage);
        setUpMetaDataForContentPage(model, multiCheckoutSummaryPage);
        model.addAttribute(WebConstants.BREADCRUMBS_KEY,
                getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.summary.breadcrumb"));
        model.addAttribute("metaRobots", "noindex,nofollow");
        setCheckoutStepLinksForModel(model, getCheckoutStep());
        return WorldlineCheckoutConstants.Views.Pages.MultiStepCheckout.worldlineCheckoutSummaryPage;
    }

    /**
     * Need to move out of controller utility method for Replenishment
     */
    protected List<String> getNumberRange(final int startNumber, final int endNumber) {
        final List<String> numbers = new ArrayList();
        for (int number = startNumber; number <= endNumber; number++) {
            numbers.add(String.valueOf(number));
        }
        return numbers;
    }

    @RequestMapping(value = "/placeOrder") // NOSONAR
    @PreValidateQuoteCheckoutStep
    @RequireHardLogIn
    public String placeOrder(@ModelAttribute("worldlinePlaceOrderForm") final WorldlinePlaceOrderForm worldlinePlaceOrderForm, final Model model,
                             HttpServletRequest request,
                             final RedirectAttributes redirectModel)
            throws CMSItemNotFoundException, InvalidCartException, CommerceCartModificationException {
        if (validateOrderForm(worldlinePlaceOrderForm, model)) {
            return enterStep(model, redirectModel);
        }

        // authorize, if failure occurs don't allow to place the order
        boolean isPaymentAuthorized = false;
        try {
            isPaymentAuthorized = getCheckoutFacade().authorizePayment(worldlinePlaceOrderForm.getSecurityCode());
        } catch (final AdapterException ae) {
            // handle a case where a wrong paymentProvider configurations on the store see getCommerceCheckoutService().getPaymentProvider()
            LOG.error(ae.getMessage(), ae);
        }
        if (!isPaymentAuthorized) {
            GlobalMessages.addErrorMessage(model, "checkout.error.authorization.failed");
            return enterStep(model, redirectModel);
        }
        final PlaceOrderData placeOrderData = new PlaceOrderData();
        placeOrderData.setNDays(worldlinePlaceOrderForm.getnDays());
        placeOrderData.setNDaysOfWeek(worldlinePlaceOrderForm.getnDaysOfWeek());
        placeOrderData.setNthDayOfMonth(worldlinePlaceOrderForm.getNthDayOfMonth());
        placeOrderData.setNWeeks(worldlinePlaceOrderForm.getnWeeks());
        placeOrderData.setReplenishmentOrder(worldlinePlaceOrderForm.isReplenishmentOrder());
        placeOrderData.setReplenishmentRecurrence(worldlinePlaceOrderForm.getReplenishmentRecurrence());
        placeOrderData.setReplenishmentStartDate(worldlinePlaceOrderForm.getReplenishmentStartDate());
        placeOrderData.setSecurityCode(worldlinePlaceOrderForm.getSecurityCode());
        placeOrderData.setTermsCheck(worldlinePlaceOrderForm.isTermsCheck());
        AbstractOrderData abstractOrderData;
        try {
            final BrowserData browserData = fillBrowserData(request, worldlinePlaceOrderForm);
            abstractOrderData = extendedCheckoutFacade.placeOrder(placeOrderData);
            String redirect = StringUtils.EMPTY;
            if (
                    (abstractOrderData instanceof OrderData && CheckoutPaymentType.CARD.getCode().equals(((OrderData) abstractOrderData).getPaymentType().getCode()))
                            || (abstractOrderData instanceof ScheduledCartData && CheckoutPaymentType.CARD.getCode().equals(((ScheduledCartData) abstractOrderData).getPaymentType().getCode()))) {
                if (BooleanUtils.isTrue(placeOrderData.getReplenishmentOrder())) {
                    redirect = worldlinePlaceOrderUtils.submitReplenishmentOrder(abstractOrderData, browserData, redirectModel);
                } else {
                    redirect = worldlinePlaceOrderUtils.submiOrder(abstractOrderData, browserData, redirectModel);
                }
            }

            if (StringUtils.isNotEmpty(redirect)) {
                return redirect;
            }

        } catch (final EntityValidationException e) {
            LOG.error("Failed to place Order", e);
            GlobalMessages.addErrorMessage(model, e.getLocalizedMessage());

            worldlinePlaceOrderForm.setTermsCheck(false);
            model.addAttribute(worldlinePlaceOrderForm);

            return enterStep(model, redirectModel);
        } catch (final Exception e) {
            LOG.error("Failed to place Order", e);
            GlobalMessages.addErrorMessage(model, "checkout.placeOrder.failed");
            return enterStep(model, redirectModel);
        }

        return redirectToOrderConfirmationPage(placeOrderData, abstractOrderData);
    }

    /**
     * Validates the order form before to filter out invalid order states
     *
     * @param placeOrderForm The spring form of the order being submitted
     * @param model          A spring Model
     * @return True if the order form is invalid and false if everything is valid.
     */
    protected boolean validateOrderForm(final WorldlinePlaceOrderForm placeOrderForm, final Model model) {
        final String securityCode = placeOrderForm.getSecurityCode();
        boolean invalid = false;

        if (getCheckoutFlowFacade().hasNoDeliveryAddress()) {
            GlobalMessages.addErrorMessage(model, "checkout.deliveryAddress.notSelected");
            invalid = true;
        }

        if (getCheckoutFlowFacade().hasNoDeliveryMode()) {
            GlobalMessages.addErrorMessage(model, "checkout.deliveryMethod.notSelected");
            invalid = true;
        }

        if (getCheckoutFlowFacade().hasNoPaymentInfo()) {
            GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.notSelected");
            invalid = true;
        } else {
            // Only require the Security Code to be entered on the summary page if the SubscriptionPciOption is set to Default.
            if (CheckoutPciOptionEnum.DEFAULT.equals(getCheckoutFlowFacade().getSubscriptionPciOption())
                    && StringUtils.isBlank(securityCode)) {
                GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.noSecurityCode");
                invalid = true;
            }
        }

        if (!placeOrderForm.isTermsCheck()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.terms.not.accepted");
            invalid = true;
            return invalid;
        }
        final CartData cartData = getCheckoutFacade().getCheckoutCart();

        if (!getCheckoutFacade().containsTaxValues()) {
            LOG.error(String.format(
                    "Cart %s does not have any tax values, which means the tax cacluation was not properly done, placement of order can't continue",
                    cartData.getCode()));
            GlobalMessages.addErrorMessage(model, "checkout.error.tax.missing");
            invalid = true;
        }

        if (!cartData.isCalculated()) {
            LOG.error(
                    String.format("Cart %s has a calculated flag of FALSE, placement of order can't continue", cartData.getCode()));
            GlobalMessages.addErrorMessage(model, "checkout.error.cart.notcalculated");
            invalid = true;
        }

        return invalid;
    }

    protected CheckoutFacade getB2BCheckoutFacade() {
        return this.extendedCheckoutFacade;
    }


    protected String getOrderCode(AbstractOrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

    @Override
    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    private BrowserData fillBrowserData(HttpServletRequest request, WorldlinePlaceOrderForm worldlinePlaceOrderForm) {

        BrowserData browserData = new BrowserData();
        browserData.setColorDepth(worldlinePlaceOrderForm.getColorDepth());
        browserData.setNavigatorJavaEnabled(worldlinePlaceOrderForm.getNavigatorJavaEnabled());
        browserData.setNavigatorJavaScriptEnabled(worldlinePlaceOrderForm.getNavigatorJavaScriptEnabled());
        browserData.setScreenHeight(worldlinePlaceOrderForm.getScreenHeight());
        browserData.setScreenWidth(worldlinePlaceOrderForm.getScreenWidth());
        browserData.setTimezoneOffsetUtcMinutes(worldlinePlaceOrderForm.getTimezoneOffset());

        browserData.setAcceptHeader(request.getHeader(ACCEPT));
        browserData.setUserAgent(request.getHeader(USER_AGENT));
        browserData.setLocale(request.getLocale().toString());
        browserData.setIpAddress(getIpAddress(request));

        return browserData;
    }

    private String getIpAddress(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader(X_FORWARDED_FOR);
            if (StringUtils.isEmpty(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }


    protected String redirectToOrderConfirmationPage(final PlaceOrderData placeOrderData, final AbstractOrderData orderData) {
        if (Boolean.TRUE.equals(placeOrderData.getReplenishmentOrder()) && (orderData instanceof ScheduledCartData)) {
            return REDIRECT_URL_REPLENISHMENT_CONFIRMATION + ((ScheduledCartData) orderData).getJobCode();
        }
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
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
        return getCheckoutStep(SUMMARY);
    }

    @InitBinder
    protected void initBinder(final ServletRequestDataBinder binder) {
        final Locale currentLocale = getI18nService().getCurrentLocale();
        final String formatString = getMessageSource().getMessage(TEXT_STORE_DATEFORMAT_KEY, null, DEFAULT_DATEFORMAT,
                currentLocale);
        final DateFormat dateFormat = new SimpleDateFormat(formatString, currentLocale);
        final CustomDateEditor editor = new CustomDateEditor(dateFormat, true);
        binder.registerCustomEditor(Date.class, editor);
    }

}
