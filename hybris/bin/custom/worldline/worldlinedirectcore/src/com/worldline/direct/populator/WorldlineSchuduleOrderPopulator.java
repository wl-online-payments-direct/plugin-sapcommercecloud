package com.worldline.direct.populator;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineSchuduleOrderPopulator implements Populator<CartToOrderCronJobModel, ScheduledCartData> {
    private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

    @Override
    public void populate(CartToOrderCronJobModel source, ScheduledCartData target) throws ConversionException {
        if (source.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {

            WorldlinePaymentInfoModel worldlinePaymentInfoModel = (WorldlinePaymentInfoModel) source.getPaymentInfo();
            target.setWorldlinePaymentInfo(worldlinePaymentInfoConverter.convert(worldlinePaymentInfoModel));
            if (target.getTriggerData() != null) {


                if (CollectionUtils.size(source.getTriggers()) > 1 && source.getTriggers().stream().map(TriggerModel::getDay).distinct().count()==1) {
                    target.getTriggerData().setDisplayTimeTable(Localization.getLocalizedString("trigger.timetable.every_month", new Object[]{source.getTriggers().iterator().next().getDay(),12 / CollectionUtils.size(source.getTriggers())}));
                }
            }
        }
    }

    @Required
    public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
        this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
    }
}
