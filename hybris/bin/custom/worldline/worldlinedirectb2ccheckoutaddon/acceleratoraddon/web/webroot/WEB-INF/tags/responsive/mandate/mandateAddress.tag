<%@ attribute name="mandateAddress" required="true" type="com.worldline.direct.address.data.WorldlineMandateAddress"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="formElement" tagdir="/WEB-INF/tags/responsive/formElement" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="theme" tagdir="/WEB-INF/tags/shared/theme" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<c:if test="${not empty mandateAddress.street}">
	${fn:escapeXml(mandateAddress.street)}
	<br>
</c:if>
<c:if test="${not empty mandateAddress.city}">
	${fn:escapeXml(mandateAddress.city)}
	<br>
</c:if>
<c:if test="${not empty mandateAddress.houseNumber}">
	${fn:escapeXml(mandateAddress.houseNumber)}
	<br>
</c:if>
<c:if test="${not empty mandateAddress.zip}">
	${fn:escapeXml(mandateAddress.zip)}
	<br>
</c:if>
<c:if test="${not empty mandateAddress.country}">
	${fn:escapeXml(mandateAddress.country)}
	<br>
</c:if>
