package com.worldline.direct.populator;

import de.hybris.platform.b2bacceleratorfacades.order.data.TriggerData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.commons.lang3.BooleanUtils;

public class WorldlineTriggerPopulator implements Populator<TriggerModel, TriggerData> {
    @Override
    public void populate(TriggerModel source, TriggerData target) throws ConversionException {
        target.setActive(BooleanUtils.isTrue(source.getActive()));
    }
}
