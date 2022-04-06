<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="paymentInfo" required="false" type="com.worldline.direct.order.data.WorldlinePaymentInfoData" %>

<%@ attribute name="savedPaymentInfos" required="false" type="java.util.List" %>

<div class="worldline_payment_product_detail display-none">
    <div class="row" style="margin-left: 0px">
        <div class="col-sm-3 col-xs-3">
            <strong><spring:theme code="checkout.paymentProduct.groupedCards.creditdebit.label"/></strong>
        </div>
        <div class="col-sm-6 col-xs-6 ">
            <strong><spring:theme code="checkout.paymentProduct.groupedCards.cartHolders.label"/></strong>
        </div>
        <div class="col-sm-3 col-xs-3 ">
            <strong><spring:theme code="checkout.paymentProduct.groupedCards.expirydate.label"/></strong>
        </div>
    </div>

    <div class="">
        <div type="button" class="btn-select" value=""></div>
        <div class="select-toggle-container">
            <ul id="worldline-selector">
                <c:forEach var="savedPaymentInfo" items="${savedPaymentInfos}" varStatus="index">
                    <c:set var="selected"
                           value="${((empty paymentInfo or (! paymentInfo.saved)) and index.index eq 0) or ((not empty paymentInfo) and paymentInfo.saved and ( paymentInfo.code eq savedPaymentInfo.code))}"/>
                    <li class="row" token="${savedPaymentInfo.token}"  ${selected?'selected':''}>
                        <div class="col-sm-3 col-xs-3">
                            <img src="${savedPaymentInfo.paymentMethodImageUrl}" alt=""/>
                        </div>
                        <div class="col-sm-6 col-xs-6 select-text">
                            <span>${savedPaymentInfo.cardholderName}</span>
                        </div>
                        <div class="col-sm-3 col-xs-3 select-text">
                            <span>${savedPaymentInfo.expiryDate}</span>
                        </div>
                    </li>
                </c:forEach>
            </ul>
        </div>
    </div>

    <form:input type="hidden" id="hostedCheckoutToken" path="hostedCheckoutToken"/>
</div>
