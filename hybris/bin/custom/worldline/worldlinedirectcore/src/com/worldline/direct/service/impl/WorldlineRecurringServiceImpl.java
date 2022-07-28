package com.worldline.direct.service.impl;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.onlinepayments.domain.GetMandateResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineRecurringService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Optional;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;

public class WorldlineRecurringServiceImpl implements WorldlineRecurringService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineRecurringServiceImpl.class);
    private WorldlinePaymentService worldlinePaymentService;

    @Override
    public Optional<CreatePaymentResponse> createRecurringPayment(AbstractOrderModel abstractOrderModel) {
        WorldlinePaymentInfoModel worldlinePaymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        switch (worldlinePaymentInfo.getId()) {
            case PAYMENT_METHOD_SEPA:
                try {
                    String mandate = worldlinePaymentInfo.getMandate();

                    GetMandateResponse mandateResponse = worldlinePaymentService.getMandate(mandate);
                    if (mandateResponse != null && WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.valueOf(mandateResponse.getMandate().getStatus()) == WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.ACTIVE) {
                        CreatePaymentResponse createPaymentResponse = worldlinePaymentService.createPayment(abstractOrderModel);
                        return Optional.of(createPaymentResponse);
                    } else {
                        return Optional.empty();
                    }

                } catch (Exception e) {
                    LOG.error("something went wrong during payment creation", e);
                    return Optional.empty();
                }
            default:
                return Optional.empty();
        }

    }

    @Override
    public void cancelRecurringPayment(CartToOrderCronJobModel cronJob) {
        PaymentInfoModel paymentInfo = cronJob.getPaymentInfo();
        if (paymentInfo instanceof WorldlinePaymentInfoModel) {
            WorldlinePaymentInfoModel worldlinePaymentInfoModel = (WorldlinePaymentInfoModel) paymentInfo;
            switch (worldlinePaymentInfoModel.getId()) {
                case PAYMENT_METHOD_SEPA: {
                    String mandate = worldlinePaymentInfoModel.getMandate();
                    GetMandateResponse revokeMandate = worldlinePaymentService.revokeMandate(mandate);
                    if (!(revokeMandate != null && WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.valueOf(revokeMandate.getMandate().getStatus()) == WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.REVOKED)) {
                        LOG.error("something went wrong while cancelling Recurring payment");
                    }
                }
                break;
            }
        }
    }

    @Required
    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }
}
