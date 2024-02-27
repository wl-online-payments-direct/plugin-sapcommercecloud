<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="formElement" tagdir="/WEB-INF/tags/responsive/formElement" %>
<%@ taglib prefix="common" tagdir="/WEB-INF/tags/responsive/common" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="displayForm" required="true" type="java.lang.Boolean" %>
<spring:htmlEscape defaultHtmlEscape="true"/>

<spring:url value="/checkout/summary/placeOrder" var="placeOrderUrl" htmlEscape="false"/>
<spring:theme code="responsive.replenishmentScheduleForm.activateDaily" var="Daily" htmlEscape="false"/>
<spring:theme code="responsive.replenishmentScheduleForm.activateWeekly" var="Weekly" htmlEscape="false"/>
<spring:theme code="responsive.replenishmentScheduleForm.activateMonthly" var="Monthly" htmlEscape="false"/>
<spring:theme code="responsive.replenishmentScheduleForm.activateYearly" var="Yearly" htmlEscape="false"/>
<spring:theme code="text.store.dateformat.datepicker.selection" text="mm/dd/yy" var="dateForForDatePicker"/>

<c:set var="styleDisplay" value="${displayForm ? 'display: block' : 'display: none'}" />

<div class="replenishment-form-container" style="${styleDisplay}">
    <div class="clearfix" id="replenishmentSchedule"
         data-date-For-Date-Picker="${dateForForDatePicker}"
         data-place-Order-Form-Replenishment-Recurrence="${fn:escapeXml(replenishmentForm.replenishmentRecurrence)}"
         data-place-Order-Form-N-Days="${fn:escapeXml(replenishmentForm.nDays)}"
         data-place-Order-Form-Nth-Day-Of-Month="${fn:escapeXml(replenishmentForm.nthDayOfMonth)}"
         data-place-Order-Form-Replenishment-Order="${replenishmentForm.replenishmentOrder}">

        <!-- Frequency replenishment data -->
        <div class="column scheduleform  scheduleform_left scheduleform-container">
            <div class="column scheduleform scheduleformD" style="display: none;">
                <div class="form-group">
                    <label class="control-label" for="nDays">
                        <spring:theme code="responsive.replenishmentScheduleForm.daily.days"/>
                    </label>
                    <div class="controls">
                        <form:select id="nDays" path="nDays" style="width: 100px;" class="form-control">
                            <form:options items="${nDays}"/>
                        </form:select>
                    </div>
                </div>
            </div>

            <div class="column scheduleform scheduleformW" style="display: none;">
                <div class="div_nWeeks1">
                    <div class="form-group">
                        <label class="control-label" for="nWeeks">
                            <spring:theme code="responsive.replenishmentScheduleForm.weekly.weeks"/>
                        </label>
                        <div class="controls">
                            <form:select id="nWeeks" path="nWeeks" style="width: 100px;" class="form-control">
                                <form:options items="${nthWeek}"/>
                            </form:select>
                        </div>
                    </div>
                </div>
            </div>

            <div class="column scheduleform scheduleformM">
                <div class="form-group">
                    <label class="control-label" for="nthDayOfMonth">
                        <spring:theme code="responsive.replenishmentScheduleForm.monthly.day"/>
                    </label>
                    <div class="controls">
                        <form:select id="nthDayOfMonth" path="nthDayOfMonth" style="width: 100px;" class="form-control">
                            <form:options items="${nthDayOfMonth}"/>
                        </form:select>
                    </div>
                </div>
            </div>
            <div class="replenishmentFrequency">
                <div class="controls">
                    <form:select id="frequency" path="replenishmentRecurrence" style="width: 100px;" class="form-control">
                        <form:option label="${Daily}" value="DAILY"/>
                        <form:option label="${Weekly}" value="WEEKLY"/>
                        <form:option label="${Monthly}" value="MONTHLY"/>
                        <form:option label="${Yearly}" value="YEARLY"/>
                    </form:select>
                </div>
            </div>
        </div>

        <!-- schedule start and end date -->
        <div class="column scheduleform  scheduleform_left">
            <div class="replenishmentFrequency_left">
                <div class="form-element-icon datepicker start">
                    <formElement:formInputBox idKey="replenishmentStartDate"
                                              labelKey="replenishmentScheduleForm.startDate"
                                              path="replenishmentStartDate" inputCSS="date js-replenishment-datepicker"
                                              mandatory="true"/>
                    <i class="glyphicon glyphicon-calendar js-open-datepicker"></i>
                    <div id="errorReplenishmentStartDate" style="display: none" class="help-block">
                        <spring:theme code="checkout.summary.placeOrder.wrongDateFormatMessage"
                                      arguments="${dateForForDatePicker}"/>
                    </div>
                </div>
            </div>
            <div class="replenishmentFrequency_left">
                <div class="form-element-icon datepicker end">
                    <formElement:formInputBox idKey="replenishmentEndDate"
                                              labelKey="replenishmentScheduleForm.endDate"
                                              path="replenishmentEndDate" inputCSS="date js-replenishment-datepicker"
                                              mandatory="true"/>
                    <i class="glyphicon glyphicon-calendar js-open-datepicker"></i>
                    <div id="errorReplenishmentEndDate" style="display: none" class="help-block">
                        <spring:theme code="checkout.summary.placeOrder.wrongDateFormatMessage"
                                      arguments="${dateForForDatePicker}"/>
                    </div>
                </div>
            </div>
            <div id="errorDatesValidity" style="display: none" class="help-block">
                <spring:theme code="responsive.replenishmentScheduleForm.invalidDates"/>
            </div>
        </div>

        <!-- Additional schedule form data -->
        <div class="column scheduleform  scheduleform_left scheduleform-container">
            <div class="column scheduleform scheduleformW" style="display: none;">
                <div class="form-group">
                    <label class="control-label">
                        <spring:theme code="responsive.replenishmentScheduleForm.weekly.daysOfWeek"/>
                    </label>
                    <div class="row scheduleform-checkboxes">
                        <form:checkboxes id="daysOfWeek" items="${daysOfWeek}" itemLabel="name" itemValue="code"
                                         path="nDaysOfWeek" element="div class='scheduleform-checkbox col-md-4 col-xs-6'"/>
                    </div>
                </div>
            </div>
            <div class="column scheduleform scheduleformM">
                <div class="form-group">
                    <label class="control-label" for="nMonths">
                        <spring:theme code="responsive.replenishmentScheduleForm.monthly.months"/>
                    </label>
                    <div class="controls">
                        <form:select id="nMonths" path="nMonths" style="width: 100px;" class="form-control">
                            <form:options items="${nthMonth}"/>
                        </form:select>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
