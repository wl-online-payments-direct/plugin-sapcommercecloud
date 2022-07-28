<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="order" tagdir="/WEB-INF/tags/responsive/order" %>
<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive" %>

<spring:htmlEscape defaultHtmlEscape="true" />

<c:if test="${not empty orderData}">
    <div class="account-orderdetail well well-tertiary">
        <div class="well-headline">
            <spring:theme code="text.account.order.orderDetails.billingInformtion" />
        </div>
        <ycommerce:testId code="orderDetails_paymentDetails_section">
            <div class="well-content">
                    <div class="row">
                        <div class="col-sm-6 order-billing-address">
                            <worldline:worldlineBillingAddressDetailsItem order="${orderData}"/>
                        </div>
                        <c:if test="${not empty orderData.worldlinePaymentInfo}">
                                <div class="col-sm-6 order-payment-data">
                                    <worldline:worldlinePaymentDetails order="${orderData}"/>
                            </div>
                        </c:if>
                    </div>
            </div>
        </ycommerce:testId>
    </div>
</c:if>