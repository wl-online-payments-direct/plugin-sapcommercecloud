package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/checkout/multi/ingenico/hosted-checkout")
public class IngenicoHostedCheckoutResponseController {

   @Resource(name = "ingenicoCheckoutFacade")
   private IngenicoCheckoutFacade ingenicoCheckoutFacade;

   @RequireHardLogIn
   @RequestMapping(value = "/response", method = {RequestMethod.POST, RequestMethod.GET})
   public String handleHostedCheckoutPaymentResponse(final HttpServletRequest request) {

      Map<String, String> requestParams = getRequestParameterMap(request);
      // get hostedCheckoutStatus and payment id
      String hostedCheckoutId = requestParams.get("hostedCheckoutId");

      if (!ingenicoCheckoutFacade.loadOrderConfirmationPageDirectly()) { // if cart doesn't exist an order exists return order confirmation page
         String cartId = requestParams.get("cartId");
         return String.format("redirect:/checkout/ingenico/orderConfirmation/%s", cartId);
      }

      //if payment is success and an order is created redirect to order confirmation page
      if (ingenicoCheckoutFacade.validatePaymentForHostedCheckoutResponse(hostedCheckoutId)) {
         if (!ingenicoCheckoutFacade.authorisePaymentHostedCheckout(hostedCheckoutId)) {
            // Payment transaction wasn't created for the order == unhappy path => don't place order
            return "redirect:/cart";
         }
         String orderId = ingenicoCheckoutFacade.startOrderCreationProcess();
         return String.format("redirect:/checkout/ingenico/orderConfirmation/%s", orderId);
      } else {
         return "redirect:/cart";
      }
   }

   protected Map<String, String> getRequestParameterMap(HttpServletRequest request) {
      Map<String, String> map = new HashMap();
      Enumeration myEnum = request.getParameterNames();

      while(myEnum.hasMoreElements()) {
         String paramName = (String)myEnum.nextElement();
         String paramValue = request.getParameter(paramName);
         map.put(paramName, paramValue);
      }

      return map;
   }

}
