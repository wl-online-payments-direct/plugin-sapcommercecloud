<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="order" tagdir="/WEB-INF/tags/responsive/order" %>
<%@ taglib prefix="mandate" tagdir="/WEB-INF/tags/addons/worldlinedirectb2bcheckoutaddon/responsive/mandate" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<c:if test="${not empty orderData && not empty orderData.worldlinePaymentInfo && not empty orderData.worldlinePaymentInfo.mandateDetail && not empty orderData.worldlinePaymentInfo.mandateDetail.customer}">
    <div class="account-orderdetail account-consignment">
        <ycommerce:testId code="replenishment_orderDetail_itemHeader_section">
            <div class="well well-quinary well-xs">
                <div class="well-headline">
                    <spring:theme code="checkout.multi.mandate.text"/>
                </div>

                <div class="well-content">
                    <div class="row">
                        <div class="col-sm-12 col-md-9">
                            <div class="row">
                                <div class="col-sm-6 col-md-4 order-ship-to">
                                    <div class="label-order">
                                        <spring:theme code="checkout.multi.mandate.info.text"/>
                                    </div>
                                    <div class="value-order">
                                        <mandate:mandateInfo
                                                mandateInfo="${orderData.worldlinePaymentInfo.mandateDetail.customer}"/>

                                    </div>
                                </div>
                                <c:if test="${not empty orderData.worldlinePaymentInfo.mandateDetail.customer.address}">
                                    <div class="col-sm-6 col-md-4 order-shipping-method">
                                        <div class="label-order">
                                            <spring:theme code="checkout.multi.mandate.address.text"/>
                                        </div>
                                        <div class="value-order">
                                            <mandate:mandateAddress
                                                    mandateAddress="${orderData.worldlinePaymentInfo.mandateDetail.customer.address}"/>

                                        </div>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </ycommerce:testId>
    </div>
</c:if>