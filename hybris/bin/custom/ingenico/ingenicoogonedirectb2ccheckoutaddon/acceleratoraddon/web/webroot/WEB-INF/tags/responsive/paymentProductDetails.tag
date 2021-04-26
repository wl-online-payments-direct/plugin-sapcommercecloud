<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer" %>
<%@ attribute name="paymentProduct" required="true" type="com.ingenico.direct.domain.PaymentProduct" %>
<%@ attribute name="isSelected" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<c:choose>
    <c:when test="${paymentProduct.id==-1}">
        <spring:theme code="checkout.paymentProduct.groupedCards.display.label" var="groupedCardsLabel"/>
        <div class="ingenico_payment_product" title="${groupedCardsLabel}">
            <form:radiobutton path="paymentProductId" value="${paymentProduct.id}" tabindex="${tabindex}"
                              checked="${isSelected ? 'checked' : '' }"/>
            <span>${groupedCardsLabel}</span>
        </div>
    </c:when>
    <c:otherwise>
        <c:set value="${idealID eq paymentProduct.id}" var="isIdeal"/>
        <div class="ingenico_payment_product" title="${paymentProduct.displayHints.label}">
            <form:radiobutton path="paymentProductId" cssClass="payment_product ${isIdeal? 'ideal':''}"
                              value="${paymentProduct.id}" tabindex="${tabindex}"
                              checked="${isSelected ? 'checked' : '' }"/>
            <span>
                <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
                ${paymentProduct.displayHints.label}
            </span>
            <c:if test="${not empty idealIssuers && isIdeal}">
                <form:select id="select_issuer" path="issuerId" cssClass="form-control" cssStyle="display: none">
                    <c:forEach var="idealIssuer" items="${idealIssuers}">
                        <option value="">
                            <spring:theme code="payment.methods.issuer.selector"/>
                        </option>
                        <form:option class="issuerOption" value="${idealIssuer.issuerId}">
                            ${idealIssuer.issuerName}
                        </form:option>
                    </c:forEach>
                </form:select>
            </c:if>
        </div>
    </c:otherwise>

</c:choose>
