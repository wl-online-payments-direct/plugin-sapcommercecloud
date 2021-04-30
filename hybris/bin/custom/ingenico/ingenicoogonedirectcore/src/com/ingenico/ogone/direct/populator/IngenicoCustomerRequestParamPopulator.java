package com.ingenico.ogone.direct.populator;

import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.Order;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

public class IngenicoCustomerRequestParamPopulator  implements Populator<CartModel, Order> {

   private CommonI18NService commonI18NService;

   @Override public void populate(CartModel cartModel, Order order) throws ConversionException {
      LanguageModel currentLanguage = cartModel.getUser().getSessionLanguage();
      String locale = commonI18NService.getLocaleForLanguage(currentLanguage).toString();

      Customer customer = new Customer();
      customer.setLocale(locale);
      order.setCustomer(customer);
   }

   public void setCommonI18NService(CommonI18NService commonI18NService) {
      this.commonI18NService = commonI18NService;
   }
}
