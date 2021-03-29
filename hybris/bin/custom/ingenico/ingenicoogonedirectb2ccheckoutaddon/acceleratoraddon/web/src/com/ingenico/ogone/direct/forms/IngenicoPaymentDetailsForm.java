
package com.ingenico.ogone.direct.forms;

import de.hybris.platform.acceleratorstorefrontcommons.forms.AddressForm;


/**
 *
 */
public class IngenicoPaymentDetailsForm {
    private int paymentProductId;

    private int issuerId;

    private boolean useDeliveryAddress;

    private boolean newBillingAddress;

    private AddressForm billingAddress;

    public int getPaymentProductId() {
        return paymentProductId;
    }

    public void setPaymentProductId(int paymentProductId) {
        this.paymentProductId = paymentProductId;
    }

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

    public int getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(int issuerId) {
        this.issuerId = issuerId;
    }
}
