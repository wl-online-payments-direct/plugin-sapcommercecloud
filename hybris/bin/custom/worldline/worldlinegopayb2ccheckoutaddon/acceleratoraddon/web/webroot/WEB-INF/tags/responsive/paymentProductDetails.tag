<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="tabindex" required="false" type="java.lang.Integer" %>
<%@ attribute name="paymentProduct" required="true" type="com.onlinepayments.domain.PaymentProduct" %>
<%@ attribute name="isSelectedID" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paymentproduct"
           tagdir="/WEB-INF/tags/addons/worldlinegopayb2ccheckoutaddon/responsive/paymentproducts" %>

<%@ attribute name="paymentInfo" required="false" type="com.worldline.gopay.order.data.WorldlinePaymentInfoData" %>

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
        <div id="worldline_payment_product_${paymentProduct.id}" class="worldline_payment_product js-worldline_payment_product ${googlePayId eq paymentProduct.id ? 'js-worldline-google-pay-product' : ''}" title="<c:out value='${paymentProduct.displayHints.label}'/>"
             data-payment-product-id="${paymentProduct.id}"
             data-google-pay-environment="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.environment) : ''}"
             data-google-pay-merchant-id="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.merchantId) : ''}"
             data-google-pay-merchant-name="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.merchantName) : ''}"
             data-google-pay-gateway="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.gateway) : ''}"
             data-google-pay-gateway-merchant-id="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.gatewayMerchantId) : ''}"
             data-google-pay-networks="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.networks) : ''}"
             data-google-pay-country-code="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.countryCode) : ''}"
             data-google-pay-currency-code="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.currencyCode) : ''}"
             data-google-pay-total-price="${googlePayId eq paymentProduct.id && googlePayConfiguration != null ? fn:escapeXml(googlePayConfiguration.totalPrice) : ''}">
            <div class="payment_product_row">
                <form:radiobutton path="paymentProductId" cssClass="payment_product"
                                  value="${paymentProduct.id}" tabindex="${tabindex}"
                                  checked="${!isSelectedStoredCard && isSelectedID ? 'checked' : '' }"/>
                <span class="payment_product">
                <c:if test="${not empty paymentProduct.displayHints.logo}">
                    <img src="${paymentProduct.displayHints.logo}" alt="<c:out value='${paymentProduct.displayHints.label}'/>"/>
                </c:if>
                ${paymentProduct.displayHints.label}
            </span>
            </div>
            <c:if test="${googlePayId eq paymentProduct.id}">
                <form:input type="hidden" path="googlePayEncryptedPaymentData" cssClass="js-worldline-google-pay-token"/>
                <form:input type="hidden" path="googlePayMobileDevice" cssClass="js-worldline-google-pay-mobile-device"/>
                <div class="js-worldline-google-pay-button display-none"></div>
            </c:if>

        </div>
    </c:otherwise>

</c:choose>
