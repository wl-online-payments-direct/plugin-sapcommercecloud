<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="paymentProducts" required="true" type="java.util.List" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive" %>

<c:set value="${not empty cartData.worldlinePaymentInfo ? cartData.worldlinePaymentInfo.id : '' }" var="selectedPaymentMethodId" />
<c:set value="${not empty cartData.worldlinePaymentInfo  ? cartData.worldlinePaymentInfo.saved : false }"
       var="isSavedCart"/>
<div id="worldline_payment_products" class="worldline_payment_products" name="worldline_payment_products">
    <c:if test="${empty paymentProducts}">
        <p><spring:theme code="payment.methods.not.found"/></p>
    </c:if>
    <c:if test="${applySurcharge && isCardPaymentMethodExisting}">
        <spring:theme code="checkout.paymentProduct.displaySurcharge.label"/>
    </c:if>
	<c:forEach items="${paymentProducts}" var="paymentProduct" varStatus="index">
        <worldline:paymentProductDetails paymentInfo="${cartData.worldlinePaymentInfo}"
                                         paymentProduct="${paymentProduct}" tabindex="${tabindex+index.count}"
                                         isSelectedID="${selectedPaymentMethodId eq paymentProduct.id}"/>
	</c:forEach>
</div>