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
<%@ taglib prefix="ingenico" tagdir="/WEB-INF/tags/addons/ingenicoogonedirectb2ccheckoutaddon/responsive" %>


<c:url value="/checkout/multi/ingenico/payment/do" var="doPayment"/>
<c:url value="/checkout/multi/ingenico/payment/view" var="viewPayment"/>
<template:page pageTitle="${pageTitle}" hideHeaderLinks="true">

    <jsp:attribute name="pageScripts">
        <script src="https://payment.preprod.direct.ingenico.com/hostedtokenization/js/client/tokenizer.min.js"></script>

        <script>
            var params = {
                validationCallback: validateHostedTokenizationSubmit,
                hideCardholderName: false
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
    </jsp:attribute>
    <jsp:attribute name="pageCss">
        <style>
            #hostedTokenization iframe[name=htpIframe0] {
                border: none;
                width: 100%;
            }
        </style>
    </jsp:attribute>

    <jsp:body>
        <div class="row">
            <div class="col-sm-6">
                <div class="checkout-headline">
                    <span class="glyphicon glyphicon-lock"></span>
                    <spring:theme code="checkout.multi.secure.checkout"/>
                </div>
                <div class="checkout-steps">
                    <a href="${viewPayment}"
                       class="step-head js-checkout-step active">
                        <div class="title">
                            <div class="headline"><spring:theme code="checkout.multi.tokenization.payment"/></div>
                        </div>
                    </a>

                    <div class="step-body">
                        <ycommerce:testId code="checkoutStepFive">
                            <div class="checkout-payment">
                                <div class="checkout-indent">
                                    <c:if test="${not empty savedPaymentInfos}">
                                        <div id="checkout-payment-tokens">
                                            <form:select id="select_payment-tokens" path="savedPaymentInfos"
                                                         cssClass="form-control">
                                                <option value="" ></option>
                                                <c:forEach var="savedPaymentInfo" items="${savedPaymentInfos}">
                                                    <form:option value="${savedPaymentInfo.token}">
                                                        ${savedPaymentInfo.cardholderName} - ${savedPaymentInfo.alias}
                                                    </form:option>
                                                </c:forEach>
                                            </form:select>
                                        </div>
                                    </c:if>
                                    <div id="hostedTokenization"></div>
                                    <form:form id="ingenicoDoPaymentForm" name="ingenicoDoPaymentForm"
                                               modelAttribute="ingenicoDoPaymentForm" method="POST"
                                               action="${doPayment}">
                                        <form:input type="hidden" path="hostedTokenizationId"/>
                                        <form:input type="hidden" path="screenHeight"/>
                                        <form:input type="hidden" path="screenWidth"/>
                                        <form:input type="hidden" path="navigatorJavaEnabled"/>
                                        <form:input type="hidden" path="timezoneOffset"/>
                                        <form:input type="hidden" path="colorDepth"/>
                                        <button type="button"
                                                class="btn btn-primary btn-block submit_ingenicoDoPaymentForm checkout-next">
                                            <spring:theme code="checkout.multi.tokenization.do.payment"/>
                                        </button>

                                    </form:form>
                                </div>
                            </div>
                        </ycommerce:testId>
                    </div>
                </div>
            </div>

            <div class="col-sm-6 hidden-xs">
                <multiCheckout:checkoutOrderDetails cartData="${cartData}" showDeliveryAddress="true"
                                                    showPaymentInfo="false" showTaxEstimate="true" showTax="true"/>
            </div>

            <div class="col-sm-12 col-lg-12">
                <cms:pageSlot position="SideContent" var="feature" element="div" class="checkout-help">
                    <cms:component component="${feature}"/>
                </cms:pageSlot>
            </div>
        </div>
    </jsp:body>
</template:page>
