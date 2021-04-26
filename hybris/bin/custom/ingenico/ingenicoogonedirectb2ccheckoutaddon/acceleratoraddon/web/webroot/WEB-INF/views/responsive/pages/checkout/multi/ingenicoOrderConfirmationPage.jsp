<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="format" tagdir="/WEB-INF/tags/shared/format" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="ingenico" tagdir="/WEB-INF/tags/addons/ingenicoogonedirectb2ccheckoutaddon/responsive" %>
<%@ taglib prefix="order" tagdir="/WEB-INF/tags/responsive/order" %>
<%@ taglib prefix="user" tagdir="/WEB-INF/tags/responsive/user"%>

<template:page pageTitle="${pageTitle}" hideHeaderLinks="true">
    <jsp:body>
        <div class="checkout-success">
        	<div class="checkout-success__body">
        		<div class="checkout-success__body__headline">
        			<spring:theme code="checkout.orderConfirmation.thankYouForOrder" />
        		</div>
        		<p><spring:theme code="text.account.order.orderNumberLabel"/><b> ${fn:escapeXml(orderData.code)}</b></p>
        		<p><spring:theme code="checkout.orderConfirmation.copySentToShort"/><b> ${fn:escapeXml(email)}</b></p>
        	</div>
        </div>
        <div class="order-detail-overview">
            <div class="row">
                <div class="col-sm-3">
                    <div class="item-group">
                        <ycommerce:testId code="orderDetail_overviewOrderID_label">
                            <span class="item-label"><spring:theme code="text.account.orderHistory.orderNumber"/></span>
                            <span class="item-value">${fn:escapeXml(orderData.code)}</span>
                        </ycommerce:testId>
                    </div>
                </div>
                <c:if test="${not empty orderData.statusDisplay}">
                    <div class="col-sm-3">
                        <div class="item-group">
                            <ycommerce:testId code="orderDetail_overviewOrderStatus_label">
                                <span class="item-label"><spring:theme code="text.account.orderHistory.orderStatus"/></span>
                                <span class="item-value"><spring:theme code="text.account.order.status.display.${orderData.statusDisplay}"/></span>
                            </ycommerce:testId>
                        </div>
                    </div>
                </c:if>
                <div class="col-sm-3">
                    <div class="item-group">
                        <ycommerce:testId code="orderDetail_overviewStatusDate_label">
                            <span class="item-label"><spring:theme code="text.account.orderHistory.datePlaced"/></span>
                            <span class="item-value"><fmt:formatDate value="${orderData.created}" dateStyle="medium" timeStyle="short" type="both"/></span>
                        </ycommerce:testId>
                    </div>
                </div>
                <div class="col-sm-3">
                    <div class="item-group">
                        <ycommerce:testId code="orderDetail_overviewOrderTotal_label">
                            <span class="item-label"><spring:theme code="text.account.order.total"/></span>
                            <span class="item-value"><format:price priceData="${orderData.totalPriceWithTax}"/></span>
                        </ycommerce:testId>
                    </div>
                </div>
                <c:if test="${orderData.quoteCode ne null}">
        			  <div class="col-sm-3">
        			  	  <div class="item-group">
        					  <spring:url htmlEscape="false" value="/my-account/my-quotes/{/quoteCode}" var="quoteDetailUrl">
        					  <spring:param name="quoteCode"  value="${orderData.quoteCode}"/>
        					  </spring:url>
        		              <ycommerce:testId code="orderDetail_overviewQuoteId_label">
        							  <span class="item-label"><spring:theme code="text.account.quote.code"/></span>
        							  <span class="item-value">
        								  <a href="${fn:escapeXml(quoteDetailUrl)}" >
        								  	  ${fn:escapeXml(orderData.quoteCode)}
        								  </a>
        							  </span>
        		              </ycommerce:testId>
        					</div>
        				</div>
        			</c:if>
            </div>
        </div>


        <%-- Add keep payment details button in this section --%>
        <div class="account-orderdetail well well-tertiary">
            <div class="well-headline">
                <spring:theme code="text.account.order.orderDetails.billingInformtion" />
            </div>
            <ycommerce:testId code="orderDetails_paymentDetails_section">
                <div class="well-content">
                    <div class="row">
                        <div class="col-sm-12 col-md-9">
                            <div class="row">
                                <div class="col-sm-6 col-md-4 order-billing-address">
                                    <ingenico:ingenicoBillingAddressDetailsItem order="${orderData}"/>
                                </div>
                                <c:if test="${not empty orderData.ingenicoPaymentInfo}">
                                    <div class="col-sm-6 col-md-4 order-payment-data">
                                        <ingenico:ingenicoPaymentDetails order="${orderData}"/>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </ycommerce:testId>
        </div>

        <order:accountOrderDetailOrderTotals order="${orderData}"/>

        <spring:url value="${continueUrl}" var="continueShoppingUrl" scope="session" htmlEscape="false"/>

        <%--<div class="row">
            <div class="pull-right col-xs-12 col-sm-6 col-md-5 col-lg-4">
                <div class="continue__shopping">
                    <button class="btn btn-primary btn-block btn--continue-shopping js-continue-shopping-button" data-continue-shopping-url="${fn:escapeXml(continueShoppingUrl)}">
                        <spring:theme code="checkout.orderConfirmation.continueShopping" />
                    </button>
                </div>
            </div>
        </div>--%>
        <c:if test="${isAnonymousUser eq true}"> <%-- Registration form --%>
            <div class="register__section">
            	<c:url value="/login/register" var="registerActionUrl" />
            	<user:register actionNameKey="register.submit"
            		action="${registerActionUrl}" />
            </div>

        </c:if>

    </jsp:body>
</template:page>