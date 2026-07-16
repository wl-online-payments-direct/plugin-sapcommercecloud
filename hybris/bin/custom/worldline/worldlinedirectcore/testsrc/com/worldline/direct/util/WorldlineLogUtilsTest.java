package com.worldline.direct.util;

import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlineLogUtilsTest {

    @Test
    public void toObfuscatedJsonSerializesZonedDateTimeWithoutReflectiveAccess() {
        String json = WorldlineLogUtils.toObfuscatedJson(new ObjectWithZonedDateTime(
              ZonedDateTime.parse("2026-07-16T11:13:20.4137739Z")));

        assertTrue(json.contains("2026-07-16T11:13:20.413773900Z"));
    }

    private static class ObjectWithZonedDateTime {
        private final ZonedDateTime transactionDate;

        ObjectWithZonedDateTime(ZonedDateTime transactionDate) {
            this.transactionDate = transactionDate;
        }
    }
}
