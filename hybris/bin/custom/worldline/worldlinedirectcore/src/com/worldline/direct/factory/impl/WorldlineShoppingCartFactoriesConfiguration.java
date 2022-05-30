package com.worldline.direct.factory.impl;

import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Required;

import java.util.Map;

public class WorldlineShoppingCartFactoriesConfiguration {
    private Map<String, WorldlineShoppingCartFactory> factoriesConfiguration;
    private WorldlineConfigurationService worldlineConfigurationService;

    public WorldlineShoppingCartFactory getShoppingCartFactory(AbstractOrderModel abstractOrderModel) {
        if (BooleanUtils.isTrue(worldlineConfigurationService.getCurrentWorldlineConfiguration().getSubmitOrderPromotion()) && abstractOrderModel.getTotalDiscounts() > 0) {
            return factoriesConfiguration.get("INCLUDE_DISCOUNT");
        } else {
            return factoriesConfiguration.get("EXCLUDE_DISCOUNT");
        }
    }

    @Required
    public void setFactoriesConfiguration(Map<String, WorldlineShoppingCartFactory> factoriesConfiguration) {
        this.factoriesConfiguration = factoriesConfiguration;
    }

    @Required
    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
