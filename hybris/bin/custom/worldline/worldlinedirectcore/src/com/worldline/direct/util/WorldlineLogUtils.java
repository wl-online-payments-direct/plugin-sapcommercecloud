package com.worldline.direct.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.time.LocalDate;

public class WorldlineLogUtils {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .create();

    public static void logAction(final Logger LOGGER, final String action, final Object params, final Object result) {
        if (LOGGER.isDebugEnabled()) {
            try {
                LOGGER.debug("[ WORLDLINE ] Action : {}", action);
                LOGGER.debug("[ WORLDLINE ] Parameters : {}", gson.toJson(params));
                LOGGER.debug("[ WORLDLINE ] Result : {}", gson.toJson(result));
            } catch (Exception e) {
                // Don't do anything - we just failed to log.
            }
        }

    }

}
