package com.worldline.gopay.service;

import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

public interface WorldlineCartToOrderService {
    void enableCartToOrderJob(CartToOrderCronJobModel cronJobModel, boolean performCronjob);

    void cancelCartToOrderJob(CartToOrderCronJobModel cronJobModel);
}
