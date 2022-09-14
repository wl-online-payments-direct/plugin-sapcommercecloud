<%@ attribute name="mandateInfo" required="true" type="com.worldline.direct.customer.data.WorldlineMandateCustomer" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="formElement" tagdir="/WEB-INF/tags/responsive/formElement" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="theme" tagdir="/WEB-INF/tags/shared/theme" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<c:if test="${not empty mandateInfo.personalInformation }">
    <c:if test="${not empty mandateInfo.personalInformation.title}">
        ${fn:escapeXml(mandateInfo.personalInformation.title)}&nbsp;
    </c:if>
    ${fn:escapeXml(mandateInfo.personalInformation.firstName)}&nbsp;${fn:escapeXml(mandateInfo.personalInformation.lastName)}
    <br>
</c:if>

<c:if test="${not empty mandateInfo.emailAddress}">
    ${fn:escapeXml(mandateInfo.emailAddress)}
    <br>
</c:if>

<c:if test="${not empty mandateInfo.companyName}">
    ${fn:escapeXml(mandateInfo.companyName)}&nbsp;
</c:if>
<c:if test="${not empty mandateInfo.iban}">
    ${fn:escapeXml(mandateInfo.iban)}
</c:if>