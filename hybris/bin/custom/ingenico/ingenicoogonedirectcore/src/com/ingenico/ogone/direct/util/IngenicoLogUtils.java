package com.ingenico.ogone.direct.util;

import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import org.slf4j.Logger;

public class IngenicoLogUtils {

    private static final GsonJsonProvider gsonJsonProvider = new GsonJsonProvider();

    public static void logAction(final Logger LOGGER, final String action, final Object params, final Object result) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ INGENICO ] Action : {}", action);
            LOGGER.debug("[ INGENICO ] Parameters : {}", gsonJsonProvider.toJson(params));
            LOGGER.debug("[ INGENICO ] Result : {}", gsonJsonProvider.toJson(result));
        }

    }

}
