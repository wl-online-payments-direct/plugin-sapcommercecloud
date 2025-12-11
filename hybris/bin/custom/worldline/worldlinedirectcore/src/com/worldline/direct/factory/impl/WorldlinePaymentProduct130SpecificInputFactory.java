package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.PaymentProduct130SpecificInput;
import com.onlinepayments.domain.PaymentProduct130SpecificThreeDSecure;
import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

import java.math.BigDecimal;

public class WorldlinePaymentProduct130SpecificInputFactory {
    public static final String CARTES_BANCAIRES_SINGLE_SALE = "single-amount";
    public static final String CARTES_BANCAIRES_SINGLE_AUTH = "payment-upon-shipment";
    public static final String CARTES_BANCAIRES_RECURRING = "other-recurring-payments";

    /**
     * Generates a PaymentProduct130SpecificInput for Cartes Bancaires. Provides numberOfItems which will represtent the
     * number of items in the AbstractOrder but is capped to 99, and populates the 3DSecure use case.
     *
     * @param abstractOrderModel Used to find the total items being ordered.
     * @param paymentInfo        Used to find whether this is a recurring payment.
     * @param isSale             Used to populate 3DSecure use case if not a recurring payment.
     * @return A PaymentProduct130SpecificInput for the provided AbstractOrderModel based on whether the order is recurring
     * and whether the current payment mode is SALE or AUTH.
     */
    public static PaymentProduct130SpecificInput getPaymentProduct130SpecificInput(WorldlineConfigurationModel config, AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo, boolean isSale) {
        if (config.getEnable3DS() == null || !config.getEnable3DS() || abstractOrderModel.getCurrency() == null) {
            return null;
        }
        PaymentProduct130SpecificInput paymentProduct130SpecificInput = new PaymentProduct130SpecificInput();
        PaymentProduct130SpecificThreeDSecure threeDSecure = new PaymentProduct130SpecificThreeDSecure();
        int numberOfItems = 0;
        for (AbstractOrderEntryModel entry : abstractOrderModel.getEntries()) {
            numberOfItems += entry.getQuantity();
        }
        threeDSecure.setNumberOfItems(Math.min(numberOfItems, 99));

        if (paymentInfo.isRecurringToken()) {
            threeDSecure.setUsecase(CARTES_BANCAIRES_RECURRING);
        } else if (isSale) {
            threeDSecure.setUsecase(CARTES_BANCAIRES_SINGLE_SALE);
        } else {
            threeDSecure.setUsecase(CARTES_BANCAIRES_SINGLE_AUTH);
        }

        threeDSecure.setAcquirerExemption(false);
        if ("EUR".equals(abstractOrderModel.getCurrency().getIsocode())) {
            if (!WorldlineExemptionType.NO_3DS_EXEMPTION.equals(config.getExemptionType3DS())) {
                if (config.getEnableMandatory3DS()) {
                    threeDSecure.setAcquirerExemption(true);
                } else {
                    BigDecimal total = BigDecimal.valueOf(abstractOrderModel.getTotalPrice());
                    BigDecimal limit = config.getExemptionLimit3DS();

                    if (limit.compareTo(total) <= 0) {
                        threeDSecure.setAcquirerExemption(true);
                    }
                }
            }

        }
        paymentProduct130SpecificInput.setThreeDSecure(threeDSecure);
        return paymentProduct130SpecificInput;
    }
}