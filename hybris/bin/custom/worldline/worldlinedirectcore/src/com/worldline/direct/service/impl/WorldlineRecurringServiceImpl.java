package com.worldline.direct.service.impl;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.onlinepayments.domain.GetMandateResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineRecurringService;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Optional;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;

public class WorldlineRecurringServiceImpl implements WorldlineRecurringService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineRecurringServiceImpl.class);
    private WorldlinePaymentService worldlinePaymentService;
    private ModelService modelService;

    @Override
    public Optional<CreatePaymentResponse> createRecurringPayment(AbstractOrderModel abstractOrderModel) {
        WorldlinePaymentInfoModel worldlinePaymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        switch (worldlinePaymentInfo.getId()) {
            case PAYMENT_METHOD_SEPA: {
                try {
                    if (worldlinePaymentInfo.getMandateDetail() != null) {
                        WorldlineMandateModel mandateDetail = worldlinePaymentInfo.getMandateDetail();
                        updateMandate(mandateDetail);
                        if (WorldlineRecurringPaymentStatus.ACTIVE.equals(mandateDetail.getStatus())) {
                            CreatePaymentResponse createPaymentResponse = worldlinePaymentService.createPayment(abstractOrderModel);
                            return Optional.of(createPaymentResponse);
                        } else {
                            return Optional.empty();
                        }

                    } else {
                        LOG.error("mandate is null for sepa direct debit");
                        return Optional.empty();

                    }

                } catch (Exception e) {
                    LOG.error("something went wrong during payment creation", e);
                    return Optional.empty();
                }
            }
            // TODO : case (CARD)
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
                case PAYMENT_METHOD_SEPA:
                default:
                {
                    WorldlineMandateModel mandateDetail = worldlinePaymentInfoModel.getMandateDetail();
                    GetMandateResponse revokeMandate = worldlinePaymentService.revokeMandate(mandateDetail);
                    if (!(revokeMandate != null && WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.valueOf(revokeMandate.getMandate().getStatus()) == WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.REVOKED)) {
                        LOG.error("something went wrong while cancelling Recurring payment");
                    }else {
                        mandateDetail.setStatus(WorldlineRecurringPaymentStatus.REVOKED);
                        modelService.save(mandateDetail);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void updateMandate(WorldlineMandateModel mandateModel) {
        GetMandateResponse mandate = worldlinePaymentService.getMandate(mandateModel);
        if (mandate != null && mandate.getMandate() != null) {
            switch (mandate.getMandate().getStatus()) {
                case "ACTIVE": {
                    mandateModel.setStatus(WorldlineRecurringPaymentStatus.ACTIVE);
                    break;
                }
                case "REVOKED": {
                    mandateModel.setStatus(WorldlineRecurringPaymentStatus.REVOKED);
                    break;
                }
                case "BLOCKED": {
                    mandateModel.setStatus(WorldlineRecurringPaymentStatus.BLOCKED);
                    break;
                }
                default: {
                    mandateModel.setStatus(WorldlineRecurringPaymentStatus.UNKNOWN);
                    break;
                }
            }
            modelService.save(mandateModel);
        }
    }

    @Override
    public void blockRecurringPayment(AbstractOrderModel abstractOrderModel) {
        WorldlinePaymentInfoModel worldlinePaymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        switch (worldlinePaymentInfo.getId()) {
            case PAYMENT_METHOD_SEPA: {
                if (worldlinePaymentInfo.getMandateDetail() != null) {
                    WorldlineMandateModel mandateDetail = worldlinePaymentInfo.getMandateDetail();
                    updateMandate(mandateDetail);
                    if (WorldlineRecurringPaymentStatus.ACTIVE.equals(mandateDetail.getStatus())) {
                        GetMandateResponse blockMandate = worldlinePaymentService.blockMandate(mandateDetail);
                        if (blockMandate != null && "BLOCKED".equals(blockMandate.getMandate().getStatus())) {
                            mandateDetail.setStatus(WorldlineRecurringPaymentStatus.BLOCKED);
                            modelService.save(mandateDetail);
                        }
                    } else {
                        LOG.warn(String.format("cannot block mandate with status = %s", mandateDetail.getStatus()));
                    }
                }
                break;
            }
            default: {
                LOG.warn("recurring Order have no recurring payment");
                break;
            }
        }
    }


    @Override
    public WorldlineRecurringPaymentStatus isRecurringPaymentEnable(CartToOrderCronJobModel cartToOrderCronJobModel) {
        if (!(cartToOrderCronJobModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
            return WorldlineRecurringPaymentStatus.ACTIVE;
        } else {
            WorldlinePaymentInfoModel worldlinePaymentInfoModel = (WorldlinePaymentInfoModel)cartToOrderCronJobModel.getPaymentInfo();
            if (WorldlinePaymentProductUtils.isPaymentSupportingRecurring(worldlinePaymentInfoModel) && worldlinePaymentInfoModel.getMandateDetail() != null) {
                return (worldlinePaymentInfoModel.getMandateDetail().getStatus());
            } else {
                return WorldlineRecurringPaymentStatus.BLOCKED;
            }
        }
    }

    @Required
    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }

    @Required
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
