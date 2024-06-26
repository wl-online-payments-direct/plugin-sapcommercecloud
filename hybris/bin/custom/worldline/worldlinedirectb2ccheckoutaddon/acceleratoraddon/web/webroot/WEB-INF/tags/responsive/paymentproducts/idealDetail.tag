<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="idealIssuers" required="true" type="java.util.List" %>

<c:if test="${not empty idealIssuers}">
    <div class="worldline_payment_product_detail display-none">
        <form:select id="select_issuer" path="issuerId" cssClass="form-control">
            <option value="" selected disabled >
                <spring:theme code="payment.methods.issuer.selector"/>
            </option>
            <c:forEach var="idealIssuer" items="${idealIssuers}">
                <form:option class="issuerOption" value="${idealIssuer.issuerId}">
                    ${idealIssuer.issuerName}
                </form:option>
            </c:forEach>
        </form:select>
    </div>
</c:if>

