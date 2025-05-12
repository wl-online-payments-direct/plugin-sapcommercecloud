package com.worldline.direct.factory.impl;

import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Required;

import java.util.Map;

public class WorldlineShoppingCartFactoriesConfiguration {
    private Map<String, WorldlineShoppingCartFactory> factoriesConfiguration;

    public WorldlineShoppingCartFactory getShoppingCartFactory(AbstractOrderModel abstractOrderModel) {
        return factoriesConfiguration.get("DEFAULT");
    }

    @Required
    public void setFactoriesConfiguration(Map<String, WorldlineShoppingCartFactory> factoriesConfiguration) {
        this.factoriesConfiguration = factoriesConfiguration;
    }
}
