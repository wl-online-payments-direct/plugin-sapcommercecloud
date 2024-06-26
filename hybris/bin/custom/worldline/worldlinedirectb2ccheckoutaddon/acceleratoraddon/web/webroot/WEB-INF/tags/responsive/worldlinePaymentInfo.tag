<%@ attribute name="cartData" required="true" type="de.hybris.platform.commercefacades.order.data.CartData" %>
<%@ attribute name="paymentInfo" required="true" type="com.worldline.direct.order.data.WorldlinePaymentInfoData " %>
<%@ attribute name="paymentProduct" required="true" type="com.onlinepayments.domain.PaymentProduct " %>
<%@ attribute name="showPaymentInfo" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<c:if test="${not empty paymentInfo && not empty paymentProduct && showPaymentInfo}">
    <ul class="checkout-order-summary-list">
        <li class="checkout-order-summary-list-heading">
            <div class="title"><spring:theme code="checkout.multi.payment" text="Payment:"/></div>
            <div class="address">
                <c:if test="${not empty paymentInfo.billingAddress}"> ${fn:escapeXml(paymentInfo.billingAddress.title)}</c:if>
                <br>
                <c:choose>
                    <c:when test="${paymentProduct.id==-1 || paymentProduct.id==-3}">
                        <spring:theme code="checkout.paymentProduct.groupedCards.display.label"
                                      var="groupedCardsLabel"/>
                        <div class="payment_product" title="${groupedCardsLabel}">
                            <c:if test="${not empty paymentProduct.displayHints.logo}">
                                <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
                            </c:if>
                            <span>${groupedCardsLabel}</span>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="payment_product" title="${paymentProduct.displayHints.label}">
                            <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
                            <span>${paymentProduct.displayHints.label}</span>
                        </div>
                    </c:otherwise>
                </c:choose>
                <c:if test="${not empty paymentInfo.billingAddress}">${fn:escapeXml(paymentInfo.billingAddress.line1)},
                    <c:if test="${not empty paymentInfo.billingAddress.line2}">${fn:escapeXml(paymentInfo.billingAddress.line2)},</c:if>
                    ${fn:escapeXml(paymentInfo.billingAddress.town)}, ${fn:escapeXml(paymentInfo.billingAddress.region.name)}&nbsp;${fn:escapeXml(paymentInfo.billingAddress.postalCode)}, ${fn:escapeXml(paymentInfo.billingAddress.country.name)}
                </c:if>
                <br/><c:if
                    test="${not empty paymentInfo.billingAddress.phone }">${fn:escapeXml(paymentInfo.billingAddress.phone)}</c:if>
            </div>
        </li>
    </ul>
</c:if>

