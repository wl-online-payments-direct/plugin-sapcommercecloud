package com.worldline.direct.service.impl;

import com.worldline.direct.service.WorldlineScheduleOrderService;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

public class WorldlineScheduleOrderServiceImpl implements WorldlineScheduleOrderService {
    private ModelService modelService;
    private CronJobService cronJobService;
    private CartService cartService;
    private TypeService typeService;
    private KeyGenerator keyGenerator;
    private static final String ACCELERATOR_CART_TO_ORDER_JOB = "worldlineAcceleratorCartToOrderJob";

    @Override
    public CartToOrderCronJobModel createOrderFromCartCronJob(CartModel cart, AddressModel deliveryAddress, AddressModel paymentAddress, PaymentInfoModel paymentInfo, List<TriggerModel> triggers) {
        final CartToOrderCronJobModel cartToOrderCronJob = modelService.create(CartToOrderCronJobModel.class);
        cartToOrderCronJob.setCart(cloneCart(cart, deliveryAddress, paymentAddress, paymentInfo));
        cartToOrderCronJob.setDeliveryAddress(deliveryAddress);
        cartToOrderCronJob.setPaymentAddress(paymentAddress);
        cartToOrderCronJob.setPaymentInfo(paymentInfo);
        cartToOrderCronJob.setJob(cronJobService.getJob(ACCELERATOR_CART_TO_ORDER_JOB));
        setCronJobToTrigger(cartToOrderCronJob, triggers);
        final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cart.getPaymentType().getCode());
        cartToOrderCronJob.setSubmitted(!cardPaymentType);
        modelService.save(cartToOrderCronJob);
        return cartToOrderCronJob;
    }

    protected CartModel cloneCart(final CartModel cart, final AddressModel deliveryAddress, final AddressModel paymentAddress,
                                  final PaymentInfoModel paymentInfo) {
        final CartModel clone = cartService.clone(typeService.getComposedTypeForClass(CartModel.class),
                typeService.getComposedTypeForClass(CartEntryModel.class), cart, keyGenerator.generate().toString());
        clone.setPaymentAddress(paymentAddress);
        clone.setDeliveryAddress(deliveryAddress);
        clone.setPaymentInfo(paymentInfo);
        clone.setUser(cart.getUser());
        return clone;
    }
    @Required
    protected void setCronJobToTrigger(final CronJobModel cronJob, final List<TriggerModel> triggers) {
        for (final TriggerModel trigger : triggers) {
            trigger.setCronJob(cronJob);
        }
        cronJob.setTriggers(triggers);
    }

    @Required
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    @Required
    public void setCronJobService(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    @Required
    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    @Required
    public void setTypeService(TypeService typeService) {
        this.typeService = typeService;
    }

    @Required
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }
}
