package com.worldline.direct.model;

import com.onlinepayments.domain.GPayThreeDSecure;

public class WorldlineGPayThreeDSecure extends GPayThreeDSecure {
    // SDK 7.4.1 lacks the acquirerExemption property required by Worldline for Google Pay TRA;
    // its Gson marshaller serializes fields declared on the runtime subclass.
    private Boolean acquirerExemption;

    public Boolean getAcquirerExemption() {
        return acquirerExemption;
    }

    public void setAcquirerExemption(Boolean acquirerExemption) {
        this.acquirerExemption = acquirerExemption;
    }
}
