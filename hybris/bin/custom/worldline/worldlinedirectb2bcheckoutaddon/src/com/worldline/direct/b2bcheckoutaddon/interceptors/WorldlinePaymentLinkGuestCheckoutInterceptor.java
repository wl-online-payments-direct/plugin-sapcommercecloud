package com.worldline.direct.b2bcheckoutaddon.interceptors;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class WorldlinePaymentLinkGuestCheckoutInterceptor implements HandlerInterceptor {

    public static final String WORLDLINE_PAY_BY_LINK_ORDER_CODE = "worldlinePayByLinkOrderCode";
    private static final String GUEST_CHECKOUT_PATH = "/login/checkout/guest";
    private static final String PAYMENT_LINK_GUEST_PATH = "/checkout/multi/worldline/payment-link/guest/";

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
            throws IOException {
        final String orderCode = getPaymentLinkOrderCode(request);
        if (isGuestCheckoutPost(request) && isNotBlank(orderCode)) {
            response.sendRedirect(request.getContextPath() + PAYMENT_LINK_GUEST_PATH + orderCode);
            return false;
        }
        return true;
    }

    private boolean isGuestCheckoutPost(final HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + GUEST_CHECKOUT_PATH).equals(request.getRequestURI());
    }

    private String getPaymentLinkOrderCode(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        final Object orderCode = session.getAttribute(WORLDLINE_PAY_BY_LINK_ORDER_CODE);
        return orderCode instanceof String ? (String) orderCode : null;
    }

    private boolean isNotBlank(final String value) {
        return value != null && !value.trim().isEmpty();
    }
}
