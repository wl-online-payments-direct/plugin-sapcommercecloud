<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="paymentProducts" required="true" type="java.util.List" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ingenico" tagdir="/WEB-INF/tags/addons/ingenicoogonedirectb2ccheckoutaddon/responsive" %>

<c:set value="${not empty cartData.ingenicoPaymentInfo ? cartData.ingenicoPaymentInfo.id : '' }" var="selectedPaymentMethodId" />
<div id="ingenico_payment_products" class="ingenico_payment_products" name="ingenico_payment_products">
    <c:if test="${empty paymentProducts}">
        <p><spring:theme code="payment.methods.not.found"/></p>
    </c:if>
	<c:forEach items="${paymentProducts}" var="paymentProduct" varStatus="index">
		<ingenico:paymentProductDetails paymentProduct="${paymentProduct}" tabindex="${tabindex+index.count}" isSelected="${selectedPaymentMethodId eq paymentProduct.id}" />
	</c:forEach>
</div>