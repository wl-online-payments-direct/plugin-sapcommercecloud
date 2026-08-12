package com.worldline.direct.service.impl;

import com.onlinepayments.DeclinedPaymentException;
import com.onlinepayments.domain.*;
import com.onlinepayments.merchant.MerchantClient;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.service.WorldlineB2BPaymentService;
import com.worldline.direct.util.WorldlineLogUtils;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.site.BaseSiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

public class WorldlineB2BPaymentServiceImpl extends WorldlinePaymentServiceImpl implements WorldlineB2BPaymentService {
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    private BaseSiteService baseSiteService;
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineB2BPaymentServiceImpl.class);

    @Override
    public CreateHostedCheckoutResponse createScheduledRecurringOrderHostedCheckout(CartToOrderCronJobModel cartToOrderCronJobModel, BrowserData browserData) {
        validateParameterNotNullStandardMessage("browserData", browserData);
        validateParameterNotNull(cartToOrderCronJobModel, "cartToOrderCronJob cannot be null");
        validateParameterNotNull(cartToOrderCronJobModel.getCart(), "cartToOrderCronJob.cart cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(cartToOrderCronJobModel.getCart());
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(browserData));
            params.getOrder().getReferences().setMerchantReference(cartToOrderCronJobModel.getCode());
            if (params.getSepaDirectDebitPaymentMethodSpecificInput() != null) {
                CreateMandateRequest mandate = params.getSepaDirectDebitPaymentMethodSpecificInput().getPaymentProduct771SpecificInput().getMandate();
                mandate.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.RECURRING.getValue());
                mandate.setCustomerReference(cartToOrderCronJobModel.getJob().getCode() + "_" + System.currentTimeMillis());
            }
            params.getHostedCheckoutSpecificInput().withIsRecurring(true);
            if (!worldlineConfigurationService.getWorldlineConfiguration(cartToOrderCronJobModel.getCart().getStore()).isFirstRecurringPayment()) {
                applyZeroAmountForTokenizedRecurringHostedCheckout(params, cartToOrderCronJobModel.getCart(), false);
            }
            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, "RESULT");
            final CreateHostedCheckoutResponse hostedCheckout = merchant.hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CreateHostedCheckoutResponse createImmediateRecurringOrderHostedCheckout(OrderModel orderModel, BrowserData browserData) {
        validateParameterNotNullStandardMessage("browserData", browserData);
        validateParameterNotNull(orderModel, "orderModel cannot be null");
        validateParameterNotNull(orderModel.getSchedulingCronJob(), "cartToOrderCronJob cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(orderModel);
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(browserData));
            if (params.getSepaDirectDebitPaymentMethodSpecificInput() != null) {
                CreateMandateRequest mandate = params.getSepaDirectDebitPaymentMethodSpecificInput().getPaymentProduct771SpecificInput().getMandate();
                mandate.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.RECURRING.getValue());
                mandate.setCustomerReference(orderModel.getSchedulingCronJob().getCode() + "_" + System.currentTimeMillis());
            }
            params.getHostedCheckoutSpecificInput().withIsRecurring(true);
            if (!orderModel.getStore().getWorldlineConfiguration().isFirstRecurringPayment()) {
                applyZeroAmountForTokenizedRecurringHostedCheckout(params, orderModel, true);
            }

            final CreateHostedCheckoutResponse hostedCheckout = merchant.hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createImmediateRecurringOrderHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CreatePaymentResponse createRecurringPaymentForImmediateReplenishmentHostedTokenization(OrderModel orderModel, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNullStandardMessage("browserData", worldlineHostedTokenizationData.getBrowserData());
        validateParameterNotNull(orderModel, "orderModel cannot be null");
        validateParameterNotNull(orderModel.getSchedulingCronJob(), "cartToOrderCronJob cannot be null");
        CreatePaymentRequest params = null;
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            params = worldlineHostedTokenizationParamConverter.convert(orderModel);
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(worldlineHostedTokenizationData.getBrowserData()));
//            params.getRedirectPaymentMethodSpecificInput().getRedirectionData().setReturnUrl(siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(),
//                    true, "/checkout/multi/worldline/hosted-tokenization/handle3ds/replenishment/" + cartToOrderCronJob.getCode()));

            if (params.getSepaDirectDebitPaymentMethodSpecificInput() != null) {
                CreateMandateWithReturnUrl mandate = params.getSepaDirectDebitPaymentMethodSpecificInput().getPaymentProduct771SpecificInput().getMandate();
                mandate.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.RECURRING.getValue());
            }
            if (!orderModel.getStore().getWorldlineConfiguration().isFirstRecurringPayment()) {
                applyZeroAmountForTokenizedRecurringSetup(params, orderModel, true);
            }

            final CreatePaymentResponse payment = merchant.payments().createPayment(params);

            WorldlineLogUtils.logAction(LOGGER, "createPaymentForImmediateReplenishmentHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            logCreatePaymentFailure(LOGGER, "createPaymentForImmediateReplenishmentHostedTokenization", params, e);
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (Exception e) {
            logCreatePaymentFailure(LOGGER, "createPaymentForImmediateReplenishmentHostedTokenization", params, e);
            LOGGER.error("[ WORLDLINE ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    public CreatePaymentResponse createRecurringPaymentForScheduledReplenishmentHostedTokenization(CartToOrderCronJobModel cartToOrderCronJob, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNull(cartToOrderCronJob, "cartToOrderCronJob cannot be null");
        validateParameterNotNull(cartToOrderCronJob.getCart(), "cartToOrderCronJob.cart cannot be null");
        CreatePaymentRequest params = null;
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            params = worldlineHostedTokenizationParamConverter.convert(cartToOrderCronJob.getCart());
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(worldlineHostedTokenizationData.getBrowserData()));
            params.getOrder().getReferences().setMerchantReference(cartToOrderCronJob.getCode());

            if (!worldlineConfigurationService.getWorldlineConfiguration(cartToOrderCronJob.getCart().getStore()).isFirstRecurringPayment()) {
                applyZeroAmountForTokenizedRecurringSetup(params, cartToOrderCronJob.getCart(), false);
            }

            if (params.getSepaDirectDebitPaymentMethodSpecificInput() != null) {
                CreateMandateWithReturnUrl mandate = params.getSepaDirectDebitPaymentMethodSpecificInput().getPaymentProduct771SpecificInput().getMandate();
                mandate.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.RECURRING.getValue());
            }

            final CreatePaymentResponse payment = merchant.payments().createPayment(params);
            WorldlineLogUtils.logAction(LOGGER, "createPaymentForScheduledReplenishmentHostedTokenization", params, payment);


            return payment;
        } catch (DeclinedPaymentException e) {
            logCreatePaymentFailure(LOGGER, "createPaymentForScheduledReplenishmentHostedTokenization", params, e);
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (Exception e) {
            logCreatePaymentFailure(LOGGER, "createPaymentForScheduledReplenishmentHostedTokenization", params, e);
            LOGGER.error("[ WORLDLINE ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    boolean applyZeroAmountForTokenizedRecurringHostedCheckout(CreateHostedCheckoutRequest params, AbstractOrderModel orderModel, boolean removeShoppingCart) {
        if (params.getCardPaymentMethodSpecificInput() != null) {
            params.getCardPaymentMethodSpecificInput().setTokenize(true);
            applyZeroAmount(params.getOrder(), orderModel, removeShoppingCart);
            return true;
        }
        if (isTokenizedHostedCheckoutGooglePayRequest(params)) {
            applyZeroAmount(params.getOrder(), orderModel, removeShoppingCart);
            return true;
        }
        return false;
    }

    boolean applyZeroAmountForTokenizedRecurringSetup(CreatePaymentRequest params, AbstractOrderModel orderModel, boolean removeShoppingCart) {
        if (params.getCardPaymentMethodSpecificInput() != null) {
            params.getCardPaymentMethodSpecificInput().setTokenize(true);
            applyZeroAmount(params.getOrder(), orderModel, removeShoppingCart);
            return true;
        }
        if (isTokenizedGooglePayRequest(params)) {
            applyZeroAmount(params.getOrder(), orderModel, removeShoppingCart);
            return true;
        }
        return false;
    }

    private boolean isTokenizedHostedCheckoutGooglePayRequest(CreateHostedCheckoutRequest params) {
        return params.getMobilePaymentMethodSpecificInput() != null
                && params.getMobilePaymentMethodSpecificInput().getPaymentProduct320SpecificInput() != null
                && Boolean.TRUE.equals(params.getMobilePaymentMethodSpecificInput().getPaymentProduct320SpecificInput().getTokenize());
    }

    private boolean isTokenizedGooglePayRequest(CreatePaymentRequest params) {
        return params.getMobilePaymentMethodSpecificInput() != null
                && params.getMobilePaymentMethodSpecificInput().getPaymentProduct320SpecificInput() != null
                && Boolean.TRUE.equals(params.getMobilePaymentMethodSpecificInput().getPaymentProduct320SpecificInput().getTokenize());
    }

    private void applyZeroAmount(Order order, AbstractOrderModel orderModel, boolean removeShoppingCart) {
        order.getAmountOfMoney().setAmount(worldlineAmountUtils.createAmount(0.0d, orderModel.getCurrency().getIsocode()));
        if (removeShoppingCart) {
            order.setShoppingCart(null);
        }
    }

    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }
}
