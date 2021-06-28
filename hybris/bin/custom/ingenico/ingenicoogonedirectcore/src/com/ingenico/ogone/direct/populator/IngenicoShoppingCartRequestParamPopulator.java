package com.ingenico.ogone.direct.populator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.LineItem;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.OrderLineDetails;
import com.ingenico.direct.domain.ShoppingCart;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class IngenicoShoppingCartRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

   private IngenicoAmountUtils ingenicoAmountUtils;

   @Override public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {

      order.setShoppingCart(createShoppingCart(abstractOrderModel));
   }

   private ShoppingCart createShoppingCart(AbstractOrderModel abstractOrderModel) {
      String currencyISOCode = abstractOrderModel.getCurrency().getIsocode();
      ShoppingCart cart = new ShoppingCart();

      List<LineItem> lineItems = new ArrayList<>();
      for (AbstractOrderEntryModel orderEntry : abstractOrderModel.getEntries()) {

         LineItem item = new LineItem();
         AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
         itemAmountOfMoney.setAmount(ingenicoAmountUtils.createAmount(orderEntry.getTotalPrice(), currencyISOCode));
         itemAmountOfMoney.setCurrencyCode(currencyISOCode);
         item.setAmountOfMoney(itemAmountOfMoney);

         item.setOrderLineDetails(createOrderLineDetails(orderEntry, currencyISOCode));
         lineItems.add(item);
      }

      //workaround for shipping taxes
      lineItems.add(setShippingAsProduct(abstractOrderModel, currencyISOCode));

      cart.setItems(lineItems);

      return cart;
   }

   private LineItem setShippingAsProduct(AbstractOrderModel abstractOrderModel, String currencyISOCode) {
      LineItem shipping = new LineItem();
      AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
      itemAmountOfMoney.setAmount(ingenicoAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));
      itemAmountOfMoney.setCurrencyCode(currencyISOCode);
      shipping.setAmountOfMoney(itemAmountOfMoney);

      OrderLineDetails orderLineDetails = new OrderLineDetails();
      orderLineDetails.setProductName(abstractOrderModel.getDeliveryMode().getName());
      orderLineDetails.setQuantity(1L);
      orderLineDetails.setProductPrice(ingenicoAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));

      shipping.setOrderLineDetails(orderLineDetails);
      return shipping;
   }

   private OrderLineDetails createOrderLineDetails(AbstractOrderEntryModel orderEntry, String currencyISOcode) {
      OrderLineDetails orderLineDetails = new OrderLineDetails();
      orderLineDetails.setProductName(orderEntry.getProduct().getName());
      orderLineDetails.setQuantity(orderEntry.getQuantity());
      orderLineDetails.setProductPrice(ingenicoAmountUtils.createAmount(orderEntry.getBasePrice(), currencyISOcode));
      return orderLineDetails;
   }

   public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
      this.ingenicoAmountUtils = ingenicoAmountUtils;
   }
}
