<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<c:set var="noBorder" value=""/>
<c:if test="${not empty worldlinePaymentInfoData}">
    <c:set var="noBorder" value="no-border"/>
</c:if>

<div class="account-section-header ${noBorder}">
    <spring:theme code="text.account.paymentDetails"/>
</div>
<c:choose>
    <c:when test="${not empty worldlinePaymentInfoData}">
        <div class="account-paymentdetails account-list">
            <div class="account-cards card-select">
                <div class="row">
                    <c:forEach items="${worldlinePaymentInfoData}" var="paymentInfo">
                        <div class="col-xs-12 col-sm-6 col-md-4 card">
                            <ul class="pull-left">
                                <li>${fn:escapeXml(paymentInfo.cardholderName)}</li>
                                <li>${fn:escapeXml(paymentInfo.cardBrand)}</li>
                                <li>${fn:escapeXml(paymentInfo.alias)}</li>
                                <li><spring:theme code="checkout.multi.tokenization.expire"
                                                  arguments="${paymentInfo.expiryDate}"/></li>
                            </ul>
                            <div class="account-cards-actions pull-left">
                                <ycommerce:testId code="paymentDetails_deletePayment_button">
                                    <a class="action-links removePaymentDetailsButton" href="#"
                                       data-payment-id="${fn:escapeXml(paymentInfo.code)}"
                                       data-popup-title="<spring:theme code="text.account.paymentDetails.delete.popup.title"/>">
                                        <span class="glyphicon glyphicon-remove"></span>
                                    </a>
                                </ycommerce:testId>
                            </div>
                        </div>

                        <div class="display-none">
                            <div id="popup_confirm_payment_removal_${fn:escapeXml(paymentInfo.code)}"
                                 class="account-address-removal-popup">
                                <spring:theme code="text.account.paymentDetails.delete.following"/>
                                <div class="address">
                                        ${fn:escapeXml(paymentInfo.cardholderName)}<br>
                                        ${fn:escapeXml(paymentInfo.cardBrand)}<br>
                                        ${fn:escapeXml(paymentInfo.alias)}<br>
                                    <spring:theme code="checkout.multi.tokenization.expire"
                                                  arguments="${paymentInfo.expiryDate}"/><br>
                                </div>
                                <c:url value="/my-account/worldline/remove-payment-detail"
                                       var="removePaymentActionUrl"/>
                                <form:form id="removePaymentDetails${paymentInfo.code}"
                                           action="${removePaymentActionUrl}"
                                           method="post">
                                    <input type="hidden" name="paymentInfoId"
                                           value="${fn:escapeXml(paymentInfo.code)}"/>
                                    <br/>
                                    <div class="modal-actions">
                                        <div class="row">
                                            <ycommerce:testId code="paymentDetailsDelete_delete_button">
                                                <div class="col-xs-12 col-sm-6 col-sm-push-6">
                                                    <button type="submit"
                                                            class="btn btn-default btn-primary btn-block paymentsDeleteBtn">
                                                        <spring:theme code="text.account.paymentDetails.delete"/>
                                                    </button>
                                                </div>
                                            </ycommerce:testId>
                                            <div class="col-xs-12 col-sm-6 col-sm-pull-6">
                                                <a class="btn btn-default closeColorBox paymentsDeleteBtn btn-block"
                                                   data-payment-id="${fn:escapeXml(paymentInfo.code)}">
                                                    <spring:theme code="text.button.cancel"/>
                                                </a>
                                            </div>
                                        </div>
                                    </div>
                                </form:form>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="account-section-content content-empty">
            <spring:theme code="text.account.paymentDetails.noPaymentInformation"/>
        </div>
    </c:otherwise>
</c:choose>
<div class="row">
    <div class="col-sm-12 col-lg-12">
        <div class="checkout-help">
            <div class="content">
                <spring:theme htmlEscape="false" code="text.account.paymentDetails.newPaymentInformation"/>
            </div>
        </div>
    </div>
</div>