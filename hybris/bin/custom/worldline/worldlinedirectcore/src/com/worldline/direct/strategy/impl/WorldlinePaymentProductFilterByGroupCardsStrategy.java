package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.PaymentProductDisplayHints;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.lang.BooleanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class WorldlinePaymentProductFilterByGroupCardsStrategy implements WorldlinePaymentProductFilterStrategy {
    private WorldlineConfigurationService worldlineConfigurationService;

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (BooleanUtils.isTrue(configuration.getGroupCards()))
        {
            List<String> groupCards = configuration.getGroupedCardsPaymentModes().stream().map(PaymentModeModel::getCode).collect(Collectors.toList());
            int paymentProductsCountBeforeFiltering = paymentProducts.size();
            paymentProducts=paymentProducts.stream().filter(paymentProduct -> !groupCards.contains(String.valueOf(paymentProduct.getId()))).collect(Collectors.toList());
            int paymentProductsCountAfterFiltering=paymentProducts.size();
            if (paymentProductsCountBeforeFiltering>paymentProductsCountAfterFiltering)
            {
                paymentProducts.add(createGroupCartPaymentProduct());
            }
        }
        return paymentProducts;
    }

    private PaymentProduct createGroupCartPaymentProduct() {
            PaymentProduct paymentProduct = new PaymentProduct();
            paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS);
            paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
            paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
            paymentProduct.getDisplayHints().setLabel(Localization.getLocalizedString("type.payment.groupedCards"));
            return paymentProduct;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
