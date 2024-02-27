package com.worldline.direct.populator;

import de.hybris.platform.b2bacceleratorfacades.order.data.TriggerData;
import de.hybris.platform.b2bacceleratorfacades.order.populators.TriggerReversePopulator;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class WorldlineTriggerReversePopulator extends TriggerReversePopulator {

    @Override
    public void populate(TriggerData data, TriggerModel model) throws ConversionException {
        super.populate(data, model);
        if (data.getDateRange() != null) {
            model.setDateRange(data.getDateRange());
        }
        model.setMonth(data.getMonth() == null ? Integer.valueOf(-1) : data.getMonth());
        model.setYear(data.getYear() == null ? Integer.valueOf(-1) : data.getYear());

    }
}
