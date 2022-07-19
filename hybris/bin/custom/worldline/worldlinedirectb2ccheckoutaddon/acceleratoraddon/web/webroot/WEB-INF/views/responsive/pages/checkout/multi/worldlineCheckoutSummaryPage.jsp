<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive" %>
<%@ taglib prefix="replenishment" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive/replenishment" %>
<%@ taglib prefix="multi-checkout" tagdir="/WEB-INF/tags/responsive/checkout/multi" %>
<%@ taglib prefix="b2b-multi-checkout" tagdir="/WEB-INF/tags/addons/b2bacceleratoraddon/responsive/checkout/multi" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<spring:url value="/checkout/multi/worldline/summary/placeOrder" var="placeOrderUrl" htmlEscape="false"/>
<spring:url value="/checkout/multi/termsAndConditions" var="getTermsAndConditionsUrl" htmlEscape="false"/>

<template:page pageTitle="${pageTitle}" hideHeaderLinks="true">
    <jsp:body>
        <div class="row">
            <div class="col-sm-6">
                <div class="checkout-headline">
                    <span class="glyphicon glyphicon-lock"></span>
                    <spring:theme code="checkout.multi.secure.checkout"/>
                </div>
                <multi-checkout:checkoutSteps checkoutSteps="${checkoutSteps}" progressBarId="${progressBarId}">
                    <ycommerce:testId code="checkoutStepFour">
                        <div class="checkout-review hidden-xs">
                            <div class="checkout-order-summary">
                                <multi-checkout:orderTotals cartData="${cartData}" showTaxEstimate="${showTaxEstimate}"
                                                            showTax="${showTax}" subtotalsCssClasses="dark"/>
                            </div>
                        </div>
                        <div class="place-order-form hidden-xs">
                            <form:form action="${placeOrderUrl}" id="placeOrderForm1"
                                       cssClass="js-worldlinePlaceOrderForm"
                                       modelAttribute="worldlinePlaceOrderForm">
                                <form:input type="hidden" path="screenHeight"/>
                                <form:input type="hidden" path="screenWidth"/>
                                <form:input type="hidden" path="navigatorJavaEnabled"/>
                                <form:input type="hidden" path="navigatorJavaScriptEnabled"/>
                                <form:input type="hidden" path="timezoneOffset"/>
                                <form:input type="hidden" path="colorDepth"/>
                                <div class="checkbox">
                                    <label> <form:checkbox id="Terms1" path="termsCheck"/>
                                        <spring:theme var="termsAndConditionsHtml"
                                                      code="checkout.summary.placeOrder.readTermsAndConditions"
                                                      arguments="${fn:escapeXml(getTermsAndConditionsUrl)}"
                                                      htmlEscape="false"/>
                                            ${ycommerce:sanitizeHTML(termsAndConditionsHtml)}
                                    </label>
                                </div>

                                <button id="placeOrder" type="submit"
                                        class="btn btn-primary btn-place-order btn-block checkout-next">
                                    <spring:theme code="checkout.summary.placeOrder" text="Place Order"/>
                                </button>
                                <c:if test="${cartData.quoteData eq null && showReplenishment eq true}">
                                    <button id="scheduleReplenishment" type="button"
                                            class="btn btn-default btn-block scheduleReplenishmentButton checkoutSummaryButton"
                                            disabled="disabled">
                                        <spring:theme code="checkout.summary.scheduleReplenishment"/>
                                    </button>

                                    <replenishment:worldlineReplenishmentScheduleForm/>
                                </c:if>
                            </form:form>
                        </div>
                    </ycommerce:testId>
                </multi-checkout:checkoutSteps>
            </div>

            <div class="col-sm-6">
                <worldline:checkoutOrderSummary cartData="${cartData}" showDeliveryAddress="true" showPaymentInfo="true"
                                                showTaxEstimate="true" showTax="true"/>
            </div>

            <div class="col-sm-12 col-lg-12">
                <br class="hidden-lg">
                <cms:pageSlot position="SideContent" var="feature" element="div" class="checkout-help">
                    <cms:component component="${feature}"/>
                </cms:pageSlot>
            </div>
        </div>

    </jsp:body>
</template:page>
