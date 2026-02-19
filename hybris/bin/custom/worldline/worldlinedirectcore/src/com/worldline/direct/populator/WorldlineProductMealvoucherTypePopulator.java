package com.worldline.direct.populator;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class WorldlineProductMealvoucherTypePopulator implements Populator<ProductModel, ProductData> {

    @Override
    public void populate(final ProductModel source, final ProductData target) throws ConversionException {
        if (source.getWorldlineMealvoucherProductType() != null) {
            String enumCode = source.getWorldlineMealvoucherProductType().getCode();
            target.setWorldlineMealvoucherProductType(enumCode);
        }
    }
}