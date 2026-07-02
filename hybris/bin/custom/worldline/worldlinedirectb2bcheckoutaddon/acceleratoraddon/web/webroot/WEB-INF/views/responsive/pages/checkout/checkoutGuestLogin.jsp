<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="user" tagdir="/WEB-INF/tags/responsive/user" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<sec:authorize access="hasAnyRole('ROLE_ANONYMOUS')">
    <c:choose>
        <c:when test="${not empty sessionScope.worldlinePayByLinkOrderCode}">
            <c:url value="/checkout/multi/worldline/payment-link/guest/${sessionScope.worldlinePayByLinkOrderCode}" var="guestCheckoutUrl" />
        </c:when>
        <c:otherwise>
            <c:url value="/login/checkout/guest" var="guestCheckoutUrl" />
        </c:otherwise>
    </c:choose>
    <user:guestCheckout actionNameKey="checkout.login.guestCheckout" action="${guestCheckoutUrl}"/>
</sec:authorize>
