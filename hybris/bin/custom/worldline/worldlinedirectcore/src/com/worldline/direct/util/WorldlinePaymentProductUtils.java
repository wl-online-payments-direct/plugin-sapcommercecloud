package com.worldline.direct.util;

import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentProductDisplayHints;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WorldlinePaymentProductUtils {
    private WorldlinePaymentModeService worldlinePaymentModeService;
    private BaseStoreService baseStoreService;

    public List<PaymentProduct> filterByAvailablePaymentModes(List<PaymentProduct> paymentProducts){
        List<String> activePaymentModeCodes = worldlinePaymentModeService.getActivePaymentModes().stream().map(PaymentModeModel::getCode).collect(Collectors.toList());
        return paymentProducts.stream()
                .filter(paymentProduct -> activePaymentModeCodes.contains(String.valueOf(paymentProduct.getId()))).collect(Collectors.toList());

    }
      public List<PaymentProduct> filterByCheckoutType(List<PaymentProduct> paymentProducts){
          final WorldlineCheckoutTypesEnum worldlineCheckoutType = getWorldlineCheckoutType();
          if (worldlineCheckoutType == null) {
              return paymentProducts;
          }
          switch (worldlineCheckoutType) {
              case HOSTED_CHECKOUT:
                  paymentProducts = paymentProducts.stream()
                          .filter(paymentProduct -> !WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod()))
                          .collect(Collectors.toList());
                  paymentProducts.add(0, createHcpGroupedCardPaymentProduct());
                  break;
              case HOSTED_TOKENIZATION:
                  final Predicate<PaymentProduct> isCard = paymentProduct -> WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod());
                  final boolean isCardsPresent = paymentProducts.stream()
                          .anyMatch(isCard);

                  final Predicate<PaymentProduct> isBCMC = paymentProduct -> WorldlinedirectcoreConstants.PAYMENT_METHOD_BCC == paymentProduct.getId();
                  paymentProducts = paymentProducts.stream()
                          .filter(isCard.negate().or(isBCMC))
                          .collect(Collectors.toList());

                  if (isCardsPresent) {
                      paymentProducts.add(0, createHtpGroupedCardPaymentProduct());
                  }
                  break;
          }
          return paymentProducts;
    }
    public WorldlineCheckoutTypesEnum getWorldlineCheckoutType() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            return currentBaseStore.getWorldlineCheckoutType();
        }
        return null;
    }

    private PaymentProduct createHtpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    private PaymentProduct createHcpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    public boolean isPaymentByKlarna(Integer paymentID)
    {
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_NOW==paymentID || WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_AFTER==paymentID;
    }
    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
