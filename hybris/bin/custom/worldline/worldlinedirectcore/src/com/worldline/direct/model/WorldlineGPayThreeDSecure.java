package com.worldline.direct.model;

import com.onlinepayments.domain.GPayThreeDSecure;

public class WorldlineGPayThreeDSecure extends GPayThreeDSecure {
    private Boolean acquirerExemption;

    public Boolean getAcquirerExemption() {
        return acquirerExemption;
    }

    public void setAcquirerExemption(Boolean acquirerExemption) {
        this.acquirerExemption = acquirerExemption;
    }
}
