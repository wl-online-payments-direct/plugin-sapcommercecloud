package com.worldline.direct.checkoutaddon.controllers.utils;

import com.worldline.direct.checkoutaddon.forms.WorldlineAddressForm;
import de.hybris.platform.acceleratorstorefrontcommons.util.AddressDataUtil;
import de.hybris.platform.commercefacades.user.data.AddressData;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component("worldlineDefaultAddressDataUtil")
public class WorldlineAddressDataUtil extends AddressDataUtil {

   public void convertBasic(final AddressData source, final WorldlineAddressForm target) {
      super.convertBasic(source, target);
      target.setCellphone(source.getCellphone());
   }

   public void convert(final AddressData source, final WorldlineAddressForm target)
   {
      convertBasic(source, target);
      target.setSaveInAddressBook(Boolean.valueOf(source.isVisibleInAddressBook()));
      target.setShippingAddress(Boolean.valueOf(source.isShippingAddress()));
      target.setBillingAddress(Boolean.valueOf(source.isBillingAddress()));
      target.setPhone(source.getPhone());

      if (source.getRegion() != null && !StringUtils.isEmpty(source.getRegion().getIsocode()))
      {
         target.setRegionIso(source.getRegion().getIsocode());
      }
   }

   public AddressData convertToAddressData(final WorldlineAddressForm addressForm)
   {
      AddressData addressData = super.convertToAddressData(addressForm);
      addressData.setCellphone(addressForm.getCellphone());

      return addressData;
   }
}
