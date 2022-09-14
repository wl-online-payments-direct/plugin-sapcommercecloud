package com.worldline.direct.service.impl;

import com.onlinepayments.Client;
import com.onlinepayments.DeclinedPaymentException;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.service.WorldlineB2BPaymentService;
import com.worldline.direct.util.WorldlineLogUtils;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.site.BaseSiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

public class WorldlineB2BPaymentServiceImpl extends WorldlinePaymentServiceImpl implements WorldlineB2BPaymentService {
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    private BaseSiteService baseSiteService;
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineB2BPaymentServiceImpl.class);

    @Override
    public CreateHostedCheckoutResponse createRecurringHostedCheckout(CartToOrderCronJobModel cartToOrderCronJobModel, BrowserData browserData) {
        validateParameterNotNullStandardMessage("browserData", browserData);
        validateParameterNotNull(cartToOrderCronJobModel, "cartToOrderCronJob cannot be null");
        validateParameterNotNull(cartToOrderCronJobModel.getCart(), "cartToOrderCronJob.cart cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {
            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(cartToOrderCronJobModel.getCart());
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(browserData));
            params.getOrder().getReferences().setMerchantReference(cartToOrderCronJobModel.getCode());
            if (params.getSepaDirectDebitPaymentMethodSpecificInput() != null) {
                CreateMandateRequest mandate = params.getSepaDirectDebitPaymentMethodSpecificInput().getPaymentProduct771SpecificInput().getMandate();
                mandate.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.RECURRING.getValue());
                params.getOrder().getAmountOfMoney().setAmount(0L);
                mandate.setCustomerReference(cartToOrderCronJobModel.getCode() + System.currentTimeMillis());
            }
            params.getHostedCheckoutSpecificInput().withIsRecurring(true);

            final CreateHostedCheckoutResponse hostedCheckout = client.merchant(getMerchantId()).hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CreatePaymentResponse createRecurringPaymentForHostedTokenization(CartToOrderCronJobModel cartToOrderCronJob, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNull(cartToOrderCronJob, "cartToOrderCronJob cannot be null");
        validateParameterNotNull(cartToOrderCronJob.getCart(), "cartToOrderCronJob.cart cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {

            final CreatePaymentRequest params = worldlineHostedTokenizationParamConverter.convert(cartToOrderCronJob.getCart());
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(worldlineHostedTokenizationData.getBrowserData()));
            params.getRedirectPaymentMethodSpecificInput().getRedirectionData().setReturnUrl(siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(),
                    true, "/checkout/multi/worldline/hosted-tokenization/handle3ds/replenishment/" + cartToOrderCronJob.getCode()));

            final CreatePaymentResponse payment = client.merchant(getMerchantId()).payments().createPayment(params);

            WorldlineLogUtils.logAction(LOGGER, "createPaymentForHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.debug(String.format("[ WORLDLINE ] Errors during getting createPayment %s", e.getMessage()));
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }
}
