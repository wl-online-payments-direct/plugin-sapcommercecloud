<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="hostedTokenization" required="true"
              type="com.ingenico.direct.domain.CreateHostedTokenizationResponse" %>
<%@ attribute name="savedPaymentInfos" required="false" type="java.util.List" %>

<c:if test="${hostedTokenization!=null}">
    <div class="ingenico_payment_product_detail display-none">

        <c:if test="${not empty savedPaymentInfos}">
            <div class="form-group">
                <button type="button"
                        class="btn btn-default btn-block js-saved-ingenico-payments">
                    <spring:theme code="checkout.multi.tokenization.useSavedCard"/>
                </button>
            </div>
            <div class="form-group display-none">
                <button type="button" class="btn btn-default btn-block js-reset-token-form">
                    <spring:theme code="checkout.multi.tokenization.resetForm"/>
                </button>
            </div>
            <div id="savedpayments">
                <div id="savedpaymentstitle">
                    <div class="headline">
                       <span class="headline-text">
                           <spring:theme code="checkout.multi.tokenization.useSavedCard"/>
                       </span>
                    </div>
                </div>
                <div id="savedpaymentsbody">
                    <c:forEach var="savedPaymentInfo" items="${savedPaymentInfos}">
                        <div class="saved-payment-entry">
                            <ul>
                                <strong>${savedPaymentInfo.cardholderName}</strong><br>
                                    ${savedPaymentInfo.cardBrand}<br>
                                    ${savedPaymentInfo.alias}<br>
                                <spring:theme code="checkout.multi.tokenization.expire"
                                              arguments="${savedPaymentInfo.expiryDate}"/><br>
                            </ul>
                            <button type="button" data-token="${savedPaymentInfo.token}"
                                    class="btn btn-primary btn-block js-use-saved-ingenico-payment">
                                <spring:theme
                                        code="checkout.multi.tokenization.useThesePaymentDetails"/>
                            </button>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>
        <div id="hostedTokenization"></div>
        <form:input type="hidden" path="hostedTokenizationId"/>
    </div>
</c:if>

