package com.worldline.direct.event.replenishment.validatecart;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.processengine.helpers.ProcessParameterHelper;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.ArrayList;
import java.util.List;

import static com.worldline.direct.actions.replenishment.WorldlineValidateCartAction.CART_MODIFICATIONS_PARAM;

public class WorldlineReplenishmentCartNonValidNotificationEventListener extends AbstractEventListener<WorldlineReplenishmentCartNonValidNotificationEvent> {
    private BusinessProcessService businessProcessService;
    private ModelService modelService;
    protected ProcessParameterHelper processParameterHelper;
    @Override
    protected void onEvent(WorldlineReplenishmentCartNonValidNotificationEvent replenishmentCartNonValidNotificationEvent) {
        ReplenishmentProcessModel originalProcess = replenishmentCartNonValidNotificationEvent.getReplenishmentProcessModel();
        List<CartModificationData> cartModifications = (List<CartModificationData>) processParameterHelper.getProcessParameterByName(originalProcess, CART_MODIFICATIONS_PARAM).getValue();

        CartToOrderCronJobModel cartToOrderCronJob = originalProcess.getCartToOrderCronJob();
        ReplenishmentProcessModel CartNonValidNotificationProcess = businessProcessService.createProcess("worldline-replenishment-cart-non-valid-email-process" + "-" + cartToOrderCronJob.getCode() + "-" + System.currentTimeMillis(), "worldline-replenishment-cart-non-valid-email-process");
        processParameterHelper.setProcessParameter(CartNonValidNotificationProcess,CART_MODIFICATIONS_PARAM,cartModifications);
        CartNonValidNotificationProcess.setCartToOrderCronJob(cartToOrderCronJob);
        CartNonValidNotificationProcess.setCustomer((CustomerModel) cartToOrderCronJob.getCart().getUser());
        CartNonValidNotificationProcess.setCurrency(cartToOrderCronJob.getCart().getCurrency());
        CartNonValidNotificationProcess.setSite(cartToOrderCronJob.getCart().getSite());
        CartNonValidNotificationProcess.setLanguage(cartToOrderCronJob.getCart().getStore().getDefaultLanguage());
        CartNonValidNotificationProcess.setStore(cartToOrderCronJob.getCart().getStore());
        modelService.save(CartNonValidNotificationProcess);
        businessProcessService.startProcess(CartNonValidNotificationProcess);

        ReplenishmentProcessModel CartNonValidMerchantNotificationProcess = businessProcessService.createProcess("worldline-replenishment-cart-non-valid-merchant-email-process" + "-" + cartToOrderCronJob.getCode() + "-" + System.currentTimeMillis(), "worldline-replenishment-cart-non-valid-merchant-email-process");
        processParameterHelper.setProcessParameter(CartNonValidMerchantNotificationProcess,CART_MODIFICATIONS_PARAM,cartModifications);
        CartNonValidMerchantNotificationProcess.setCartToOrderCronJob(cartToOrderCronJob);
        CartNonValidMerchantNotificationProcess.setCustomer((CustomerModel) cartToOrderCronJob.getCart().getUser());
        CartNonValidMerchantNotificationProcess.setCurrency(cartToOrderCronJob.getCart().getCurrency());
        CartNonValidMerchantNotificationProcess.setLanguage(cartToOrderCronJob.getCart().getStore().getDefaultLanguage());
        CartNonValidMerchantNotificationProcess.setStore(cartToOrderCronJob.getCart().getStore());
        CartNonValidMerchantNotificationProcess.setSite(cartToOrderCronJob.getCart().getSite());
        modelService.save(CartNonValidMerchantNotificationProcess);
        businessProcessService.startProcess(CartNonValidMerchantNotificationProcess);

    }

    public void setBusinessProcessService(BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setProcessParameterHelper(ProcessParameterHelper processParameterHelper) {
        this.processParameterHelper = processParameterHelper;
    }
}
