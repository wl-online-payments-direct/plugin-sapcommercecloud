package com.worldline.gopay.factory.impl;

import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.factory.WorldlineShoppingCartFactory;
import com.worldline.gopay.service.WorldlineConfigurationService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Required;

import java.util.Map;

public class WorldlineShoppingCartFactoriesConfiguration {
    private Map<String, WorldlineShoppingCartFactory> factoriesConfiguration;

    public WorldlineShoppingCartFactory getShoppingCartFactory(AbstractOrderModel abstractOrderModel) {
        if (abstractOrderModel.getPaymentMode() != null && String.valueOf(WorldlinegopaycoreConstants.PAYMENT_METHOD_MEALVOUCHER)
                .equals(abstractOrderModel.getPaymentMode().getCode())) {
            return factoriesConfiguration.get("MEALVOUCHERS");
        }
        return factoriesConfiguration.get("DEFAULT");
    }

    @Required
    public void setFactoriesConfiguration(Map<String, WorldlineShoppingCartFactory> factoriesConfiguration) {
        this.factoriesConfiguration = factoriesConfiguration;
    }
}
