package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.PaymentProductDisplayHints;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.lang.BooleanUtils;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;

import java.util.List;
import java.util.stream.Collectors;

import static com.worldline.direct.enums.WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT;

public class WorldlinePaymentProductFilterByGroupCardsStrategy implements WorldlinePaymentProductFilterStrategy {
   private WorldlineConfigurationService worldlineConfigurationService;

   private BaseStoreService baseStoreService;

   @Override
   public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
      final WorldlineCheckoutTypesEnum worldlineCheckoutType = getWorldlineCheckoutType();
      if (worldlineCheckoutType != null && HOSTED_CHECKOUT.equals(worldlineCheckoutType)) {
         WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
         if (BooleanUtils.isTrue(configuration.getGroupCards())) {
            int paymentProductsCountBeforeFiltering = paymentProducts.size();
            paymentProducts = paymentProducts.stream().filter(paymentProduct -> paymentProduct.getPaymentProductGroup() == null).collect(Collectors.toList());
            int paymentProductsCountAfterFiltering = paymentProducts.size();
            if (paymentProductsCountBeforeFiltering > paymentProductsCountAfterFiltering) {
               paymentProducts.add(1, createGroupCartPaymentProduct()); // index 0 will be saved cards
            }
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

   public WorldlineCheckoutTypesEnum getWorldlineCheckoutType() {
      final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
      if (currentBaseStore != null) {
         return currentBaseStore.getWorldlineCheckoutType();
      }
      return null;
   }

   public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
      this.worldlineConfigurationService = worldlineConfigurationService;
   }

   public void setBaseStoreService(BaseStoreService baseStoreService) {
      this.baseStoreService = baseStoreService;
   }
}
