package com.worldline.direct.service.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

@UnitTest
public class WorldlineTransactionServiceImplTest {

    @Test
    public void getPaymentIdKeepsShortWorldlinePaymentIds() throws Exception {
        assertEquals("12345678901", getPaymentId("12345678901"));
    }

    @Test
    public void getPaymentIdKeepsSuffixBeforeCaptureSeparator() throws Exception {
        assertEquals("1234567890", getPaymentId("1234567890_1"));
    }

    @Test
    public void getPaymentIdPreservesLegacyLongIdStripping() throws Exception {
        assertEquals("1234567890", getPaymentId("0000001234567890"));
    }

    private String getPaymentId(String rawPaymentTransactionId) throws Exception {
        Method method = WorldlineTransactionServiceImpl.class.getDeclaredMethod("getPaymentId", String.class);
        method.setAccessible(true);
        return (String) method.invoke(new WorldlineTransactionServiceImpl(), rawPaymentTransactionId);
    }
}
