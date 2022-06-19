<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer" %>
<%@ attribute name="paymentProduct" required="true" type="com.onlinepayments.domain.PaymentProduct" %>
<%@ attribute name="isSelectedID" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paymentproduct"
           tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive/paymentproducts" %>
<%@ attribute name="paymentInfo" required="false" type="com.worldline.direct.order.data.WorldlinePaymentInfoData" %>
<c:set var="isSelectedStoredCard" value="${not empty paymentInfo.savedPayment}" />
<c:choose>
    <c:when test="${paymentProduct.id==-1}">
        <div class="worldline_payment_product htp" style="margin-bottom:-7px" title="${groupedCardsLabel}">
            <paymentproduct:hostedTokenizationDetail
                    paymentProductId="${paymentProduct.id}"
                    paymentInfo="${paymentInfo}"
                    hostedTokenization="${hostedTokenization}"
                    savedPaymentInfos="${savedPaymentInfos}"/>
        </div>
        <form:input type="hidden" path="savedCardCode"/>

    </c:when>
    <c:when test="${paymentProduct.id==-2}">
        <div class="worldline_payment_product" title="${groupedCardsLabel}">
            <paymentproduct:hostedCheckoutDetail paymentProductId="${paymentProduct.id}" paymentInfo="${paymentInfo}" savedPaymentInfos="${savedPaymentInfos}"/>
        </div>
        <form:input type="hidden" path="savedCardCode"/>
    </c:when>
    <c:otherwise>
        <div id="worldline_payment_product_${paymentProduct.id}" class="worldline_payment_product js-worldline_payment_product ${applePayId eq paymentProduct.id? 'display-none' : ''}" title="${paymentProduct.displayHints.label}">
            <div class="payment_product_row">
                <form:radiobutton path="paymentProductId" cssClass="payment_product"
                                  value="${paymentProduct.id}" tabindex="${tabindex}"
                                  checked="${!isSelectedStoredCard && isSelectedID ? 'checked' : '' }"/>
                <span class="payment_product">
                <c:if test="${not empty paymentProduct.displayHints.logo}">
                    <img src="${paymentProduct.displayHints.logo}" alt="${paymentProduct.displayHints.label}"/>
                </c:if>
                ${paymentProduct.displayHints.label}
            </span>
                <c:if test="${idealID eq paymentProduct.id}">
                    <paymentproduct:idealDetail idealIssuers="${idealIssuers}"/>
                </c:if>
            </div>

        </div>
    </c:otherwise>

</c:choose>
