<%@ taglib prefix="worldline" tagdir="/WEB-INF/tags/addons/worldlinedirectb2bcheckoutaddon/responsive" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<form:form modelAttribute="worldlinePaymentDetailsForm">
    <worldline:billingAddressFormElements regions="${regions}"
                                          country="${country}"/>
</form:form>
