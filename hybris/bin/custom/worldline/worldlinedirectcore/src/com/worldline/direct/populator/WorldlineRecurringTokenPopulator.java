package com.worldline.direct.populator;

import com.worldline.direct.model.WorldlineRecurringTokenModel;
import com.worldline.direct.order.data.WorldlineRecurringTokenData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.commons.lang.StringUtils;

public class WorldlineRecurringTokenPopulator implements Populator<WorldlineRecurringTokenModel, WorldlineRecurringTokenData> {

   private EnumerationService enumerationService;

   @Override
   public void populate(WorldlineRecurringTokenModel worldlineRecurringTokenModel, WorldlineRecurringTokenData worldlineRecurringTokenData) throws ConversionException {
      worldlineRecurringTokenData.setRecurringToken(worldlineRecurringTokenModel.getToken());
      worldlineRecurringTokenData.setStatus(enumerationService.getEnumerationName(worldlineRecurringTokenModel.getStatus()));
      worldlineRecurringTokenData.setSubscriptionId(worldlineRecurringTokenModel.getSubscriptionID());

      worldlineRecurringTokenData.setAlias(formattedAlias(worldlineRecurringTokenModel.getAlias()));
      worldlineRecurringTokenData.setCardholderName(worldlineRecurringTokenModel.getCardholderName());
      worldlineRecurringTokenData.setExpiryDate(worldlineRecurringTokenModel.getExpiryDate());
      String[] splittedDate = StringUtils.split(worldlineRecurringTokenModel.getExpiryDate(), "/");
      if (splittedDate != null && splittedDate.length == 2) {
         worldlineRecurringTokenData.setExpiryMonth(splittedDate[0]);
         worldlineRecurringTokenData.setExpiryYear(splittedDate[1]);
      }
      worldlineRecurringTokenData.setCustomer(worldlineRecurringTokenModel.getCustomer());
   }

   private String formattedAlias(String alias) {
      if (StringUtils.isNotEmpty(alias) && alias.length()>4) {
         return "*".repeat(12) + alias.substring(alias.length() - 4);
      }else {
         return StringUtils.EMPTY;
      }
   }

   public void setEnumerationService(EnumerationService enumerationService) {
      this.enumerationService = enumerationService;
   }
}
