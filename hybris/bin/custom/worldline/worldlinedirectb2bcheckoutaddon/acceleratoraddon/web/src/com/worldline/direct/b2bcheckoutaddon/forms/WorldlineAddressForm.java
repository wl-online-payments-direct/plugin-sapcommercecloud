package com.worldline.direct.b2bcheckoutaddon.forms;

import de.hybris.platform.acceleratorstorefrontcommons.forms.AddressForm;

public class WorldlineAddressForm extends AddressForm {
   private String cellphone;

   public String getCellphone() {
      return cellphone;
   }

   public void setCellphone(String cellphone) {
      this.cellphone = cellphone;
   }
}
