<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="replenishment" tagdir="/WEB-INF/tags/addons/worldlinedirectb2ccheckoutaddon/responsive/replenishment" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<c:if test="${not empty orderData}">
    <div class="account-orderdetail well well-tertiary well-single-headline replenishment-order-history">
        <div class="well-headline">
            <spring:theme code="text.account.replenishment.history"/>
        </div>
    </div>

    <spring:url value="/my-account/worldline/my-replenishment/{/jobCode}" var="searchUrl" htmlEscape="false">
	    <spring:param name="jobCode"  value="${orderData.jobCode}"/>
        <spring:param name="sort" value="${searchPageData.pagination.sort}" />
        <spring:param name="show" value="${param.show}" />
    </spring:url>

    <replenishment:orderListing searchUrl="${searchUrl}"
                            messageKey="text.account.orderHistory.page"></replenishment:orderListing>
</c:if>