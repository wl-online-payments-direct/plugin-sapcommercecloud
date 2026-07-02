package com.worldline.direct.b2bcheckoutaddon.controllers.pages.steps;

import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.security.GUIDCookieStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.PayByLink.root)
public class WorldlinePaymentLinkResponseController extends AbstractCheckoutController {

    public static final String WORLDLINE_PAY_BY_LINK_ORDER_CODE = "worldlinePayByLinkOrderCode";
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";

    @Resource(name = "guidCookieStrategy")
    private GUIDCookieStrategy guidCookieStrategy;

    @RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.PayByLink.handleResponse + ORDER_CODE_PATH_VARIABLE_PATTERN,
            method = {RequestMethod.GET, RequestMethod.POST})
    public String handlePaymentLinkResponse(@PathVariable("orderCode") final String orderCode) {
        getSessionService().setAttribute(WORLDLINE_PAY_BY_LINK_ORDER_CODE, orderCode);
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + orderCode;
    }

    @RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.PayByLink.guest + ORDER_CODE_PATH_VARIABLE_PATTERN,
            method = {RequestMethod.GET, RequestMethod.POST})
    public String continueAsGuest(@PathVariable("orderCode") final String orderCode,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) {
        getSessionService().setAttribute(WebConstants.ANONYMOUS_CHECKOUT, Boolean.TRUE);
        getSessionService().setAttribute(WebConstants.ANONYMOUS_CHECKOUT_GUID, orderCode);
        getSessionService().removeAttribute(WORLDLINE_PAY_BY_LINK_ORDER_CODE);
        guidCookieStrategy.setCookie(request, response);
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + orderCode;
    }
}
