<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags" %>
<%@ taglib prefix="multiCheckout" tagdir="/WEB-INF/tags/responsive/checkout/multi" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="formElement" tagdir="/WEB-INF/tags/responsive/formElement" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive" %>


<c:url value="/checkout/multi/worldline/select-payment-method" var="selectPaymentMethod"/>

<template:page pageTitle="${pageTitle}" hideHeaderLinks="true">

    <jsp:attribute name="pageScripts">
        <c:if test="${hostedTokenization!=null}">
        <script src="${hostedTokenizationJs}"></script>
            <script>
                var params = {
                    validationCallback: validateHostedTokenizationSubmit,
                    hideCardholderName: false,
                    hideTokenFields: false
                };
                var tokenizer = new Tokenizer("${hostedTokenization.partialRedirectUrl}", 'hostedTokenization', params);

                tokenizer.initialize()
                    .then(() => {
                        console.log("tokenizer", "initialized");
                    })
                    .catch(reason => {
                        console.log("tokenizer", "error on init");
                    });

                function validateHostedTokenizationSubmit(result) {
                    console.log("tokenizer", result);
                }
            </script>
        </c:if>
    </jsp:attribute>

    <jsp:body>
        <div class="row">
            <div class="col-sm-6">
                <div class="checkout-headline">
                    <span class="glyphicon glyphicon-lock"></span>
                    <spring:theme code="checkout.multi.secure.checkout"/>
                </div>
                <multiCheckout:checkoutSteps checkoutSteps="${checkoutSteps}" progressBarId="${progressBarId}">
                    <jsp:body>

                        <ycommerce:testId code="checkoutStepThree">
                            <div class="checkout-paymentmethod">
                                <div class="checkout-indent">

                                    <div class="headline"><spring:theme code="checkout.multi.worldlinePaymentMethod.paymentMethod.subtitle"/></div>

                                    <form:form id="worldlineSelectPaymentForm" name="worldlineSelectPaymentForm"
                                               modelAttribute="worldlinePaymentDetailsForm" method="POST"
                                               action="${selectPaymentMethod}">
                                        <c:set var="tab_index" value="1" scope="page"/>
                                        <worldline:paymentProductSelector paymentProducts="${paymentProducts}"/>

                                        <div class="headline">
                                            <spring:theme
                                                    code="checkout.multi.worldlinePaymentMethod.billingAddress.subtitle"/>
                                        </div>

                                        <c:if test="${cartData.deliveryItemsQuantity > 0}">
                                            <div id="useWorldlineDeliveryAddressData"
                                                 data-title="${fn:escapeXml(cartData.deliveryAddress.title)}"
                                                 data-firstname="${fn:escapeXml(cartData.deliveryAddress.firstName)}"
                                                 data-lastname="${fn:escapeXml(cartData.deliveryAddress.lastName)}"
                                                 data-line1="${fn:escapeXml(cartData.deliveryAddress.line1)}"
                                                 data-line2="${fn:escapeXml(cartData.deliveryAddress.line2)}"
                                                 data-town="${fn:escapeXml(cartData.deliveryAddress.town)}"
                                                 data-postalcode="${fn:escapeXml(cartData.deliveryAddress.postalCode)}"
                                                 data-countryisocode="${fn:escapeXml(cartData.deliveryAddress.country.isocode)}"
                                                 data-regionisocode="${fn:escapeXml(cartData.deliveryAddress.region.isocodeShort)}"
                                                 data-address-id="${fn:escapeXml(cartData.deliveryAddress.id)}"
                                            ></div>
                                            <formElement:formCheckbox
                                                    path="useDeliveryAddress"
                                                    idKey="useWorldlineDeliveryAddress"
                                                    labelKey="checkout.multi.sop.useMyDeliveryAddress"
                                                    tabindex="${tab_index+1}"/>


                                        </c:if>

                                        <worldline:billAddressFormSelector supportedCountries="${countries}"
                                                                          regions="${regions}"
                                                                          tabindex="${tab_index+2}"/>
                                        <p><spring:theme
                                                code="checkout.multi.paymentMethod.seeOrderSummaryForMoreInformation"/></p>

                                        <button type="button"
                                                class="btn btn-primary btn-block submit_worldlineSelectPaymentForm checkout-next">
                                            <spring:theme code="checkout.multi.paymentMethod.continue"/></button>

                                    </form:form>
                                </div>
                            </div>
                        </ycommerce:testId>
                    </jsp:body>
                </multiCheckout:checkoutSteps>
            </div>

            <div class="col-sm-6 hidden-xs">
                <multiCheckout:checkoutOrderDetails cartData="${cartData}" showDeliveryAddress="true"
                                                    showPaymentInfo="false" showTaxEstimate="false" showTax="true"/>
            </div>

            <div class="col-sm-12 col-lg-12">
                <cms:pageSlot position="SideContent" var="feature" element="div" class="checkout-help">
                    <cms:component component="${feature}"/>
                </cms:pageSlot>
            </div>
        </div>
    </jsp:body>
</template:page>
