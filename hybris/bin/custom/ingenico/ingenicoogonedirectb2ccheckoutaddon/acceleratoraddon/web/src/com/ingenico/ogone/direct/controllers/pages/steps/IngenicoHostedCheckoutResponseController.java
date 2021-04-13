package com.ingenico.ogone.direct.controllers.pages.steps;

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

      //TODO this controller will be covered as part of ING-633
      //it is for handling the response after the hosted checkout is complete
      Map<String, String> requestParams = getRequestParameterMap(request);
      // get hostedCheckoutStatus and payment id
      String hostedCheckoutId = requestParams.get("hostedCheckoutId");

      //if payment is success and an order is created redirect to order confirmation page
      if (ingenicoCheckoutFacade.validatePaymentForHostedCheckoutResponse(hostedCheckoutId)) {
         // todo fill model with order data
      } else {
         return "redirect:/cart";
      }

//      return "redirect:/checkout/ingenico/orderConfirmation/%s";
      return "redirect:/";
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
