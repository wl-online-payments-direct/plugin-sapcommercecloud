package com.worldline.gopay.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.PaymentProductDisplayHints;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.WorldlineCheckoutTypesEnum;
import com.worldline.gopay.evaluate.WorldlinePaymentProductEvaluator;
import com.worldline.gopay.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WorldlinePaymentProductFilterByCheckoutStrategy implements WorldlinePaymentProductFilterStrategy {
    private List<WorldlinePaymentProductEvaluator> worldlineHostedCheckoutPaymentProductsEvaluatorList;
    private List<WorldlinePaymentProductEvaluator> worldlineHostedTokenizationPaymentProductsEvaluatorList;
    private BaseStoreService baseStoreService;

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        final WorldlineCheckoutTypesEnum worldlineCheckoutType = getWorldlineCheckoutType();
        if (worldlineCheckoutType == null || CollectionUtils.isEmpty(paymentProducts)) {
            return paymentProducts;
        }

        switch (worldlineCheckoutType) {
            case HOSTED_CHECKOUT:
                paymentProducts = paymentProducts.stream()
                        .filter(paymentProduct -> worldlineHostedCheckoutPaymentProductsEvaluatorList.stream().anyMatch(pr -> pr.evaluate().test(paymentProduct)))
                        .collect(Collectors.toList());
                if (paymentProducts.get(0).getId() != WorldlinegopaycoreConstants.PAYMENT_METHOD_HCP) {
                    paymentProducts.add(0, createHcpGroupedCardPaymentProduct());
                }
                break;
            case HOSTED_TOKENIZATION:
                final Predicate<PaymentProduct> isCard = paymentProduct -> WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod());
                final boolean isCardsPresent = paymentProducts.stream()
                        .anyMatch(isCard);


                paymentProducts = paymentProducts.stream()
                        .filter(paymentProduct -> worldlineHostedTokenizationPaymentProductsEvaluatorList.stream().anyMatch(pr -> pr.evaluate().test(paymentProduct)))
                        .collect(Collectors.toList());

                if (isCardsPresent && (paymentProducts.size() == 0 || paymentProducts.get(0).getId() != WorldlinegopaycoreConstants.PAYMENT_METHOD_HTP)) {
                    paymentProducts.add(0, createHtpGroupedCardPaymentProduct());
                }
                break;
        }
        return paymentProducts;
    }

    private PaymentProduct createHtpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinegopaycoreConstants.PAYMENT_METHOD_HTP);
        paymentProduct.setPaymentMethod(WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel(Localization.getLocalizedString("type.payment.byCard"));
        return paymentProduct;
    }

    private PaymentProduct createHcpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinegopaycoreConstants.PAYMENT_METHOD_HCP);
        paymentProduct.setPaymentMethod(WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("");
        return paymentProduct;
    }

    public WorldlineCheckoutTypesEnum getWorldlineCheckoutType() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            return currentBaseStore.getWorldlineCheckoutType();
        }
        return null;
    }

    public void setWorldlineHostedCheckoutPaymentProductsEvaluatorList(List<WorldlinePaymentProductEvaluator> worldlineHostedCheckoutPaymentProductsEvaluatorList) {
        this.worldlineHostedCheckoutPaymentProductsEvaluatorList = worldlineHostedCheckoutPaymentProductsEvaluatorList;
    }

    public void setWorldlineHostedTokenizationPaymentProductsEvaluatorList(List<WorldlinePaymentProductEvaluator> worldlineHostedTokenizationPaymentProductsEvaluatorList) {
        this.worldlineHostedTokenizationPaymentProductsEvaluatorList = worldlineHostedTokenizationPaymentProductsEvaluatorList;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
