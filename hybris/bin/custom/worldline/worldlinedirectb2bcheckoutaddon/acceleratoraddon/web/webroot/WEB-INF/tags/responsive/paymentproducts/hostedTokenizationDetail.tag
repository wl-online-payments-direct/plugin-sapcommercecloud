<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="hostedTokenization" required="true"
              type="com.onlinepayments.domain.CreateHostedTokenizationResponse" %>
<%@ attribute name="savedPaymentInfos" required="false" type="java.util.List" %>
<%@ attribute name="paymentProductId" required="false" type="java.lang.Integer" %>
<%@ attribute name="paymentInfo" required="false" type="com.worldline.direct.order.data.WorldlinePaymentInfoData" %>
<c:set var="isChecked" value="${false}"/>

<div class="js-hostedTokenization-partialRedirectUrl" value="${hostedTokenization.partialRedirectUrl}"></div>

<fieldset class="credit-card-selector-block form-check">
    <table>
        <body>
        <c:if test="${not empty savedPaymentInfos}">
            <tr class="card-row selected">
                <td colspan="5">
                    <spring:theme code="checkout.paymentProduct.savedCard"/>
                </td>
            </tr>
        </c:if>

        <c:forEach var="savedPaymentInfo" items="${savedPaymentInfos}" varStatus="index">
            <c:set var="checked"
                   value="${((not empty paymentInfo) and ( paymentInfo.savedPayment eq savedPaymentInfo.code))}"/>
            <c:set var="isChecked" value="${isChecked?true:checked}"/>
            <tr class="card-row ">
                <td class="radioButton">
                    <form:radiobutton path="paymentProductId"
                                      value="${paymentProductId}"
                                      is-saved="true"
                                      cssClass="radio_image_row"
                                      code="${savedPaymentInfo.code}"
                                      token="${savedPaymentInfo.token}"
                                      checked="${checked ? 'checked' : '' }"/>
                    <img src="${savedPaymentInfo.paymentMethodImageUrl}" alt="" class="logo">
                </td>
                <td class="paymentProductName">
                        ${savedPaymentInfo.cardBrand}
                </td>
                <td class="creditCardHolder">
                        ${savedPaymentInfo.cardholderName}
                </td>
                <td class="maskedCreditCardNumber">
                    <span class="creditCardHolder">${savedPaymentInfo.cardholderName}</span>
                        ${savedPaymentInfo.alias}
                </td>
                <td class="creditCardExpiration">
                        ${savedPaymentInfo.expiryDate}
                </td>
            </tr>
            <tr class="display-none">
                <td colspan="5">
                    <div id="hostedTokenization-${index.index}" name="hostedTokenization-${index.index}"
                         class="hostedTokenization"></div>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${not empty savedPaymentInfos}">
            <tr class="card-row selected">
                <td colspan="5">
                    <spring:theme code="checkout.paymentProduct.otherPayments"/>
                </td>
            </tr>
        </c:if>

        <tr class="card-row">
            <td class="radioButton" colspan="5">
                <form:radiobutton path="paymentProductId"
                                  value="${paymentProductId}"
                                  is-saved="false"
                                  checked="${!isChecked ? 'checked' : '' }"/>
                <span class="new-cart"><spring:theme code="checkout.paymentProduct.tokenization.newCard"/></span>
            </td>
        </tr>
        <tr class="display-none">
            <td colspan="5">
                <div id="hostedTokenization-new" name="hostedTokenization-new" class="hostedTokenization"></div>
                <c:if test="${displayReplenishmentMessage}">
                    <spring:theme code="checkout.paymentProduct.replenishOrderMessage.label"/>
                </c:if>
            </td>
        </tr>
        </body>
    </table>
</fieldset>


<form:input type="hidden" path="hostedTokenizationId"/>
