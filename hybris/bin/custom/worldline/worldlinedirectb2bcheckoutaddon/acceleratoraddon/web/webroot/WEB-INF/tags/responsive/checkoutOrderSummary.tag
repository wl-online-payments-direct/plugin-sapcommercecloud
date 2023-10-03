<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="cartData" required="true" type="de.hybris.platform.commercefacades.order.data.CartData" %>
<%@ attribute name="showDeliveryAddress" required="true" type="java.lang.Boolean" %>
<%@ attribute name="showPaymentInfo" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showTax" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showTaxEstimate" required="false" type="java.lang.Boolean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="multi-checkout" tagdir="/WEB-INF/tags/responsive/checkout/multi" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="order" tagdir="/WEB-INF/tags/responsive/order" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2bcheckoutaddon/responsive" %>
<%@ taglib prefix="worldline-multi-checkout" tagdir="/WEB-INF/tags/addons/worldlinedirectb2bcheckoutaddon/responsive/checkout/multi" %>


<spring:htmlEscape defaultHtmlEscape="true"/>

<spring:url value="/checkout/multi/worldline/summary/placeOrder" var="placeOrderUrl" htmlEscape="false"/>
<spring:url value="/checkout/multi/termsAndConditions" var="getTermsAndConditionsUrl" htmlEscape="false"/>

<div class="checkout-summary-headline hidden-xs">
    <spring:theme code="checkout.multi.order.summary"/>
</div>
<div class="checkout-order-summary">
    <ycommerce:testId code="orderSummary">
        <multi-checkout:deliveryCartItems cartData="${cartData}" showDeliveryAddress="${showDeliveryAddress}"/>

        <c:forEach items="${cartData.pickupOrderGroups}" var="groupData" varStatus="status">
            <multi-checkout:pickupCartItems cartData="${cartData}" groupData="${groupData}" showHead="true"/>
        </c:forEach>

        <order:appliedVouchers order="${cartData}"/>

        <worldline:worldlinePaymentInfo cartData="${cartData}" paymentInfo="${cartData.worldlinePaymentInfo}"
                                        paymentProduct="${paymentProduct}" showPaymentInfo="${showPaymentInfo}"/>


        <worldline-multi-checkout:orderTotals cartData="${cartData}" showTaxEstimate="${showTaxEstimate}" showTax="${showTax}"/>
    </ycommerce:testId>
</div>

<div class="visible-xs clearfix">
    <form:form action="${placeOrderUrl}" id="worldlinePlaceOrderForm" modelAttribute="worldlinePlaceOrderForm"
               class="place-order-form col-xs-12">
        <form:input type="hidden" path="screenHeight"/>
        <form:input type="hidden" path="screenWidth"/>
        <form:input type="hidden" path="navigatorJavaEnabled"/>
        <form:input type="hidden" path="navigatorJavaScriptEnabled"/>
        <form:input type="hidden" path="timezoneOffset"/>
        <form:input type="hidden" path="colorDepth"/>
        <c:if test="${cartData.quoteData eq null && tokenizePayment eq true}">
            <div class="checkbox">
                <label> <form:checkbox id="saveCardDetails" path="cardDetailsCheck" />
                    <spring:theme code="checkout.multi.order.saveCardDetails"/>
                </label>
            </div>
        </c:if>
        <div class="checkbox">
            <label> <form:checkbox id="Terms1" path="termsCheck"/>
                <spring:theme var="termsAndConditionsHtml" code="checkout.summary.placeOrder.readTermsAndConditions"
                              arguments="${fn:escapeXml(getTermsAndConditionsUrl)}" htmlEscape="false"/>
                    ${ycommerce:sanitizeHTML(termsAndConditionsHtml)}
            </label>
        </div>

        <button id="placeOrder" type="submit" class="btn btn-primary btn-place-order btn-block">
            <spring:theme code="checkout.summary.placeOrder"/>
        </button>
    </form:form>
</div>
