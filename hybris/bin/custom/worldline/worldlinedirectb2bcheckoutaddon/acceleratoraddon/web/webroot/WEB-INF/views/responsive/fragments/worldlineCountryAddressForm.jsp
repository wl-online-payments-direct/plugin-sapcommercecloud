<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2bcheckoutaddon/responsive/address" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:if test="${not empty country}">
	<form:form modelAttribute="addressForm">
		<worldline:addressFormElements regions="${regions}"
		                             country="${country}"/>
	</form:form>
</c:if>
