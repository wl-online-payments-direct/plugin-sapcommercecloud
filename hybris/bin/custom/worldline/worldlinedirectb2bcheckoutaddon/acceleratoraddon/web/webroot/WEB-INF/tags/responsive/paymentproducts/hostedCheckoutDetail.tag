<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="paymentInfo" required="false" type="com.worldline.direct.order.data.WorldlinePaymentInfoData" %>
<%@ attribute name="savedPaymentInfos" required="false" type="java.util.List" %>
<%@ attribute name="paymentProductId" required="false" type="java.lang.Integer" %>

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
            <c:set var="selected"
                   value="${((not empty paymentInfo) and ( paymentInfo.savedPayment eq savedPaymentInfo.code))}"/>
            <tr class="card-row ">
                <td class="radioButton">
                    <form:radiobutton path="paymentProductId"
                                      cssClass="radio_image_row"
                                      value="${paymentProductId}"
                                      is-saved="true"
                                      token="${savedPaymentInfo.token}"
                                      code="${savedPaymentInfo.code}"
                                      checked="${selected ? 'checked' : '' }"/>
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
        </c:forEach>
        <c:if test="${not empty savedPaymentInfos}">
            <tr class="card-row selected">
                <td colspan="5">
                    <spring:theme code="checkout.paymentProduct.otherPayments"/>
                </td>
            </tr>
        </c:if>
        </body>
    </table>
</fieldset>
