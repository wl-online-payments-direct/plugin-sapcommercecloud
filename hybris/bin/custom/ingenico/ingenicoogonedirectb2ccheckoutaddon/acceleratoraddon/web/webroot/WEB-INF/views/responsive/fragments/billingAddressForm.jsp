<%@ taglib prefix="ingenico" tagdir="/WEB-INF/tags/addons/ingenicoogonedirectb2ccheckoutaddon/responsive" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<form:form modelAttribute="ingenicoPaymentDetailsForm">
    <ingenico:billingAddressFormElements regions="${regions}"
                                        country="${country}"/>
</form:form>
