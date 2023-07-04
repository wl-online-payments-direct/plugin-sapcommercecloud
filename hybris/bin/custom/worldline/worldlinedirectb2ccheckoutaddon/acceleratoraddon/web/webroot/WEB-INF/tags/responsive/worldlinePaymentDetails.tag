<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="order" required="true" type="de.hybris.platform.commercefacades.order.data.AbstractOrderData" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:htmlEscape defaultHtmlEscape="true"/>
<div class="label-order">
    <spring:theme code="text.account.paymentType"/>
</div>
<div class="value-order">
    ${fn:escapeXml(order.worldlinePaymentInfo.paymentMethod)}<br/>
    ${fn:escapeXml(order.worldlinePaymentInfo.cardholderName)}<br/>
    ${fn:escapeXml(order.worldlinePaymentInfo.alias)}<br/>
    <c:if test="${not empty orderData.worldlinePaymentInfo.expiryDate}">
        <spring:theme code="checkout.multi.tokenization.expire"
            arguments="${order.worldlinePaymentInfo.expiryDate}"/>
    </c:if>
</div>

