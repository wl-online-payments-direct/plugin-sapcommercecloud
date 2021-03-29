<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer"%>
<%@ attribute name="paymentProduct" required="true" type="com.ingenico.direct.domain.PaymentProduct" %>
<%@ attribute name="isSelected" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:choose>
    <c:when test="${paymentProduct.id==-1}">
        <spring:theme code="checkout.paymentProduct.groupedCards.display.label" var="groupedCardsLabel"/>
        <div class="ingenico_payment_product" title="${groupedCardsLabel}">
            <form:radiobutton path="paymentProductId" value="${paymentProduct.id}" tabindex="${tabindex}" checked="${isSelected ? 'checked' : '' }"/>
            <img src="" alt="${groupedCardsLabel}"/>
            <span>${groupedCardsLabel}</span>
        </div>
    </c:when>
    <c:otherwise>
        <div class="ingenico_payment_product" title="${paymentProduct.displayHints.label}">
            <form:radiobutton path="paymentProductId" value="${paymentProduct.id}" tabindex="${tabindex}" checked="${isSelected ? 'checked' : '' }"/>
            <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
            <span>${paymentProduct.displayHints.label}</span>
        </div>
    </c:otherwise>

</c:choose>
