<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer" %>
<%@ attribute name="paymentProduct" required="true" type="com.ingenico.direct.domain.PaymentProduct" %>
<%@ attribute name="isSelected" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paymentproduct"
           tagdir="/WEB-INF/tags/addons/ingenicoogonedirectb2ccheckoutaddon/responsive/paymentproducts" %>

<c:choose>
    <c:when test="${paymentProduct.id==-1}">
        <spring:theme code="checkout.paymentProduct.groupedCards.display.label" var="groupedCardsLabel"/>
        <div class="ingenico_payment_product" title="${groupedCardsLabel}">
            <form:radiobutton path="paymentProductId" cssClass="payment_product htp"  value="${paymentProduct.id}" tabindex="${tabindex}"
                              checked="${isSelected ? 'checked' : '' }"/>
            <span>${groupedCardsLabel}</span>
            <paymentproduct:hostedTokenizationDetail hostedTokenization="${hostedTokenization}"
                                                     savedPaymentInfos="${savedPaymentInfos}"/>
        </div>
    </c:when>
    <c:otherwise>
        <div class="ingenico_payment_product" title="${paymentProduct.displayHints.label}">
            <form:radiobutton path="paymentProductId" cssClass="payment_product"
                              value="${paymentProduct.id}" tabindex="${tabindex}"
                              checked="${isSelected ? 'checked' : '' }"/>
            <span>
                <c:if test="${not empty paymentProduct.displayHints.logo}">
                    <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
                </c:if>
                ${paymentProduct.displayHints.label}
            </span>
            <c:if test="${idealID eq paymentProduct.id}">
                <paymentproduct:idealDetail idealIssuers="${idealIssuers}"/>
            </c:if>
        </div>
    </c:otherwise>

</c:choose>
