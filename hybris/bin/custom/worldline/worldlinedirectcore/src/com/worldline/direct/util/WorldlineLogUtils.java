package com.worldline.direct.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.onlinepayments.logging.BodyObfuscator;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public class WorldlineLogUtils {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
            .create();
    private static final BodyObfuscator bodyObfuscator = BodyObfuscator.custom()
            .obfuscateAll("encryptedPaymentData")
            .obfuscateAll("hostedTokenizationId")
            .obfuscateAll("token")
            .build();

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

    public static void logFailedAction(final Logger LOGGER, final String action, final Object params, final Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            try {
                LOGGER.debug("[ WORLDLINE ] Failed Action : {}", action);
                LOGGER.debug("[ WORLDLINE ] Failed Parameters : {}", toObfuscatedJson(params));
                LOGGER.debug("[ WORLDLINE ] Failure : {} - {}", throwable.getClass().getName(), throwable.getMessage());
            } catch (Exception e) {
                // Don't do anything - we just failed to log.
            }
        }
    }

    public static String toObfuscatedJson(final Object value) {
        return obfuscate(gson.toJson(value));
    }

    public static String obfuscate(final String value) {
        return bodyObfuscator.obfuscateBody(value);
    }

    private static class ZonedDateTimeTypeAdapter implements JsonSerializer<ZonedDateTime> {
        @Override
        public JsonElement serialize(final ZonedDateTime zonedDateTime, final Type type, final JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(zonedDateTime.toString());
        }
    }

}
