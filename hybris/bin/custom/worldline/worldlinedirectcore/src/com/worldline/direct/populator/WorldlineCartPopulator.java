package com.worldline.direct.populator;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import java.math.BigDecimal;

public class WorldlineCartPopulator implements Populator<CartModel, CartData> {

    private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

    private PriceDataFactory priceDataFactory;
    @Override
    public void populate(final CartModel source, final CartData target) throws ConversionException {
        final PaymentInfoModel paymentInfo = source.getPaymentInfo();
        if (paymentInfo instanceof WorldlinePaymentInfoModel) {
            WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();
            worldlinePaymentInfoConverter.convert((WorldlinePaymentInfoModel) paymentInfo, worldlinePaymentInfoData);
            target.setWorldlinePaymentInfo(worldlinePaymentInfoData);
            if (source.getPaymentCost() > 0.0d) {
                target.setSurcharge(priceDataFactory.create(PriceDataType.BUY, new BigDecimal(source.getPaymentCost()), source.getCurrency()));
            }
        }
        if (source.isWorldlineReplenishmentOrder()) {
            target.setReplenishmentOrder(source.isWorldlineReplenishmentOrder());
            target.setNDays(source.getWorldlineNDays());
            target.setNDaysOfWeek(source.getWorldlineNDaysOfWeek());
            target.setNthDayOfMonth(source.getWorldlineNthDayOfMonth());
            target.setNWeeks(source.getWorldlineNWeeks());
            target.setNMonths(source.getWorldlineNMonths());
            target.setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum.valueOf(source.getWorldlineReplenishmentRecurrence().getCode()));
            target.setReplenishmentStartDate(source.getWorldlineReplenishmentStartDate());
            target.setReplenishmentEndDate(source.getWorldlineReplenishmentEndDate());
        }
    }

    public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
        this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
    }

    public void setPriceDataFactory(PriceDataFactory priceDataFactory) {
        this.priceDataFactory = priceDataFactory;
    }
}
