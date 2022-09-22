package com.worldline.direct.event;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import de.hybris.platform.servicelayer.model.ModelService;

public class WorldlineReplenishmentPaymentFailedNotificationEventListener extends AbstractEventListener<WorldlineReplenishmentPaymentFailedNotificationEvent> {
    private BusinessProcessService businessProcessService;
    private ModelService modelService;
    @Override
    protected void onEvent(WorldlineReplenishmentPaymentFailedNotificationEvent replenishmentPaymentFailedNotificationEvent) {
        ReplenishmentProcessModel originalProcess = replenishmentPaymentFailedNotificationEvent.getReplenishmentProcessModel();
        CartToOrderCronJobModel cartToOrderCronJob = originalProcess.getCartToOrderCronJob();
        ReplenishmentProcessModel paymentFailedCustomerNotificationProcess = businessProcessService.createProcess("worldline-replenishment-payment-failed-customer-email-process" + "-" + cartToOrderCronJob.getCode() + "-" + System.currentTimeMillis(), "worldline-replenishment-payment-failed-customer-email-process");
        paymentFailedCustomerNotificationProcess.setCartToOrderCronJob(cartToOrderCronJob);
        paymentFailedCustomerNotificationProcess.setCustomer((CustomerModel) cartToOrderCronJob.getCart().getUser());
        paymentFailedCustomerNotificationProcess.setCurrency(cartToOrderCronJob.getCart().getCurrency());
        paymentFailedCustomerNotificationProcess.setLanguage(cartToOrderCronJob.getCart().getStore().getDefaultLanguage());
        paymentFailedCustomerNotificationProcess.setStore(cartToOrderCronJob.getCart().getStore());
        modelService.save(paymentFailedCustomerNotificationProcess);
        businessProcessService.startProcess(paymentFailedCustomerNotificationProcess);

        ReplenishmentProcessModel paymentFailedMerchantNotificationProcess = businessProcessService.createProcess("worldline-replenishment-payment-failed-merchant-email-process" + "-" + cartToOrderCronJob.getCode() + "-" + System.currentTimeMillis(), "worldline-replenishment-payment-failed-merchant-email-process");
        paymentFailedMerchantNotificationProcess.setCartToOrderCronJob(cartToOrderCronJob);
        paymentFailedMerchantNotificationProcess.setCustomer((CustomerModel) cartToOrderCronJob.getCart().getUser());
        paymentFailedMerchantNotificationProcess.setCurrency(cartToOrderCronJob.getCart().getCurrency());
        paymentFailedMerchantNotificationProcess.setLanguage(cartToOrderCronJob.getCart().getStore().getDefaultLanguage());
        paymentFailedMerchantNotificationProcess.setStore(cartToOrderCronJob.getCart().getStore());
        modelService.save(paymentFailedMerchantNotificationProcess);
        businessProcessService.startProcess(paymentFailedMerchantNotificationProcess);

    }

    public void setBusinessProcessService(BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

}
