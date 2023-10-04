<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="cart" tagdir="/WEB-INF/tags/responsive/cart" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="replenishment" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive/replenishment" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>

<spring:htmlEscape defaultHtmlEscape="true" />
<c:url value="/cart/checkout" var="checkoutUrl" scope="session"/>
<c:url value="/cart/savePlaceOrderData" var="replenishmentFormUrl" scope="session"/>
<div class="row">
    <div class="col-xs-12 col-sm-10 col-md-5 col-lg-6 pull-left cart-actions--print">

        <div class="express-checkout">
            <sec:authorize access="hasAnyRole('ROLE_ANONYMOUS')" var="isAnonymous"/>
            <div class="headline"><spring:theme code="text.basket.auto.replenish.block"/></div>
            <c:choose>
                <c:when test="${isAnonymous}">
                    <div class="checkbox">
                        <label style="color: rgba(50,54,58,0.6)">
                            <input type="checkbox" disabled/>
                            <spring:theme code="text.order.type.checkbox" />
                        </label>
                    </div>
                    <p>
                        <spring:theme code="text.basket.anonymous.replenish.form" />
                    </p>
                </c:when>
                <c:otherwise>
                    <form:form action="${replenishmentFormUrl}" id="replenishmentForm"
                           name="replenishmentForm"
                           cssClass="js-replenishmentForm"
                           modelAttribute="replenishmentForm"
                           method="POST">
                        <div class="checkbox">
                            <label> <form:checkbox id="replenishmentOrder" path="replenishmentOrder"/>
                                <spring:theme code="text.order.type.checkbox"/>
                            </label>
                        </div>
                        <replenishment:worldlineReplenishmentScheduleForm displayForm="${replenishmentForm.replenishmentOrder}"/>
                    </form:form>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="col-xs-12 col-sm-10 col-md-7 col-lg-6 pull-right cart-actions--print">
        <div class="express-checkout">
            <div class="headline"><spring:theme code="text.expresscheckout.header"/></div>
            <strong><spring:theme code="text.expresscheckout.title"/></strong>
            <ul>
                <li><spring:theme code="text.expresscheckout.line1"/></li>
                <li><spring:theme code="text.expresscheckout.line2"/></li>
                <li><spring:theme code="text.expresscheckout.line3"/></li>
            </ul>
            <sec:authorize access="isFullyAuthenticated()">
                <c:if test="${expressCheckoutAllowed}">
                    <div class="checkbox">
                        <label>
                            <c:url value="/checkout/multi/express" var="expressCheckoutUrl" scope="session"/>
                            <input type="checkbox" class="express-checkout-checkbox" data-express-checkout-url="${fn:escapeXml(expressCheckoutUrl)}">
                            <spring:theme text="I would like to Express checkout" code="cart.expresscheckout.checkbox"/>
                        </label>
                     </div>
                </c:if>
           </sec:authorize>
        </div>
    </div>
</div>

<div class="cart__actions">
    <div class="row">
        <div class="col-sm-4 col-md-3 pull-right">
            <ycommerce:testId code="checkoutButton">
                <button class="btn btn-primary btn-block btn--continue-checkout js-worldline-continue-checkout-button" data-checkout-url="${fn:escapeXml(checkoutUrl)}">
                    <spring:theme code="checkout.checkout"/>
                </button>
            </ycommerce:testId>
        </div>

        <sec:authorize access="!hasAnyRole('ROLE_ANONYMOUS')">
            <c:if test="${not empty siteQuoteEnabled and siteQuoteEnabled eq 'true'}">
                <div class="col-sm-4 col-md-3 col-md-offset-3 pull-right">
                    <button class="btn btn-default btn-block btn--continue-shopping js-continue-shopping-button"    data-continue-shopping-url="${fn:escapeXml(createQuoteUrl)}">
                        <spring:theme code="quote.create"/>
                    </button>
                </div>
            </c:if>
        </sec:authorize>

        <div class="col-sm-4 col-md-3 pull-right">
            <button class="btn btn-default btn-block btn--continue-shopping js-continue-shopping-button" data-continue-shopping-url="${fn:escapeXml(continueShoppingUrl)}">
                <spring:theme code="cart.page.continue"/>
            </button>
        </div>
    </div>
</div>


<c:if test="${showCheckoutStrategies && not empty cartData.entries}" >
    <div class="cart__actions">
        <div class="row">
            <div class="col-xs-12 col-sm-5 col-md-3 col-lg-2 pull-right">
                <input type="hidden" name="flow" id="flow"/>
                <input type="hidden" name="pci" id="pci"/>
                <select id="selectAltCheckoutFlow" class="doFlowSelectedChange form-control">
                    <option value="select-checkout"><spring:theme code="checkout.checkout.flow.select"/></option>
                    <option value="multistep"><spring:theme code="checkout.checkout.multi"/></option>
                    <option value="multistep-pci"><spring:theme code="checkout.checkout.multi.pci"/></option>
                </select>
                <select id="selectPciOption" class="display-none">
                    <option value=""><spring:theme code="checkout.checkout.multi.pci.select"/></option>
                    <c:if test="${!isOmsEnabled}">
                        <option value="default"><spring:theme code="checkout.checkout.multi.pci-ws"/></option>
                        <option value="hop"><spring:theme code="checkout.checkout.multi.pci-hop"/></option>
                    </c:if>
                    <option value="sop"><spring:theme code="checkout.checkout.multi.pci-sop" text="PCI-SOP" /></option>
                </select>
            </div>
        </div>
    </div>
</c:if>
