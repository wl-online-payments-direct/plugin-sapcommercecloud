package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.PaymentProductDisplayHints;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.lang.BooleanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class WorldlinePaymentProductFilterByGroupCardsStrategy implements WorldlinePaymentProductFilterStrategy {
    private WorldlineConfigurationService worldlineConfigurationService;

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (BooleanUtils.isTrue(configuration.getGroupCards())) {
            int paymentProductsCountBeforeFiltering = paymentProducts.size();
            paymentProducts = paymentProducts.stream().filter(paymentProduct -> paymentProduct.getPaymentProductGroup() == null).collect(Collectors.toList());
            int paymentProductsCountAfterFiltering = paymentProducts.size();
            if (paymentProductsCountBeforeFiltering > paymentProductsCountAfterFiltering) {
                paymentProducts.add(1, createGroupCartPaymentProduct()); // index 0 will be saved cards
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
            if (worldlineConfigurationService.getCurrentWorldlineConfiguration().getGroupCardsLogo() != null) {
               paymentProduct.getDisplayHints().setLogo(worldlineConfigurationService.getCurrentWorldlineConfiguration().getGroupCardsLogo().getURL());
            }
            return paymentProduct;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
