package com.worldline.direct.util;

import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import org.slf4j.Logger;

public class WorldlineLogUtils {

    private static final GsonJsonProvider gsonJsonProvider = new GsonJsonProvider();

    public static void logAction(final Logger LOGGER, final String action, final Object params, final Object result) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ WORLDLINE ] Action : {}", action);
            LOGGER.debug("[ WORLDLINE ] Parameters : {}", gsonJsonProvider.toJson(params));
            LOGGER.debug("[ WORLDLINE ] Result : {}", gsonJsonProvider.toJson(result));
        }

    }

}
