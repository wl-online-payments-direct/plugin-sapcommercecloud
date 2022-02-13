
package com.worldline.direct.checkoutaddon.forms;

import de.hybris.platform.acceleratorstorefrontcommons.forms.AddressForm;


/**
 *
 */
public class WorldlinePaymentDetailsForm {
    private Integer paymentProductId;

    private String hostedTokenizationId;

    private String issuerId;

    private boolean useDeliveryAddress;

    private boolean newBillingAddress;

    private AddressForm billingAddress;

    public Integer getPaymentProductId() {
        return paymentProductId;
    }

    public void setPaymentProductId(Integer paymentProductId) {
        this.paymentProductId = paymentProductId;
    }

    public String getHostedTokenizationId() {
        return hostedTokenizationId;
    }

    public void setHostedTokenizationId(String hostedTokenizationId) {
        this.hostedTokenizationId = hostedTokenizationId;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
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
}
