package com.worldline.direct.b2bcheckoutaddon.interceptors;

import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlinePaymentLinkGuestCheckoutInterceptorTest {

    private final WorldlinePaymentLinkGuestCheckoutInterceptor interceptor = new WorldlinePaymentLinkGuestCheckoutInterceptor();

    @Test
    public void redirectsGuestCheckoutPostToPaymentLinkGuestFlowWhenOrderMarkerExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login/checkout/guest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession().setAttribute(WorldlinePaymentLinkGuestCheckoutInterceptor.WORLDLINE_PAY_BY_LINK_ORDER_CODE, "ORDER-123");

        boolean result = interceptor.preHandle(request, response, null);

        assertFalse(result);
        assertEquals("/checkout/multi/worldline/payment-link/guest/ORDER-123", response.getRedirectedUrl());
    }

    @Test
    public void allowsGuestCheckoutPostWhenNoPaymentLinkOrderMarkerExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login/checkout/guest");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    public void allowsNonPostRequestsWhenPaymentLinkOrderMarkerExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/checkout/guest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession().setAttribute(WorldlinePaymentLinkGuestCheckoutInterceptor.WORLDLINE_PAY_BY_LINK_ORDER_CODE, "ORDER-123");

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result);
        assertNull(response.getRedirectedUrl());
    }
}
