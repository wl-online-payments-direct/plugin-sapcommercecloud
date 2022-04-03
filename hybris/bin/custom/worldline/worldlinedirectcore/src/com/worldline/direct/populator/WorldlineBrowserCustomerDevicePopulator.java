package com.worldline.direct.populator;

import com.ingenico.direct.domain.CustomerDevice;
import com.worldline.direct.order.data.BrowserData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class WorldlineBrowserCustomerDevicePopulator implements Populator<BrowserData, CustomerDevice> {
    @Override
    public void populate(BrowserData source, CustomerDevice target) throws ConversionException {
        com.ingenico.direct.domain.BrowserData browserData = new com.ingenico.direct.domain.BrowserData();
        browserData.setColorDepth(source.getColorDepth());
        browserData.setJavaEnabled(source.getNavigatorJavaEnabled());
        browserData.setJavaScriptEnabled(source.getNavigatorJavaScriptEnabled());
        browserData.setScreenHeight(source.getScreenHeight());
        browserData.setScreenWidth(source.getScreenWidth());
        target.setAcceptHeader(source.getAcceptHeader());
        target.setUserAgent(source.getUserAgent());
        target.setLocale(source.getLocale());
        target.setIpAddress(source.getIpAddress());
        target.setTimezoneOffsetUtcMinutes(source.getTimezoneOffsetUtcMinutes());
        target.setBrowserData(browserData);
    }
}
