
package com.ingenico.ogone.direct.forms;

import de.hybris.platform.acceleratorstorefrontcommons.forms.AddressForm;


/**
 *
 */
public class IngenicoPaymentDetailsForm {
    private Integer paymentProductId;

    private Integer issuerId;

    private boolean useDeliveryAddress;

    private boolean newBillingAddress;

    private AddressForm billingAddress;

    public boolean isUseDeliveryAddress() {
        return useDeliveryAddress;
    }

    public void setUseDeliveryAddress(boolean useDeliveryAddress) {
        this.useDeliveryAddress = useDeliveryAddress;
    }

    public boolean isNewBillingAddress() {
        return newBillingAddress;
    }

    public void setNewBillingAddress(boolean newBillingAddress) {
        this.newBillingAddress = newBillingAddress;
    }

    public AddressForm getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(AddressForm billingAddress) {
        this.billingAddress = billingAddress;
    }

    public Integer getPaymentProductId() {
        return paymentProductId;
    }

    public void setPaymentProductId(Integer paymentProductId) {
        this.paymentProductId = paymentProductId;
    }

    public Integer getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(Integer issuerId) {
        this.issuerId = issuerId;
    }
}
