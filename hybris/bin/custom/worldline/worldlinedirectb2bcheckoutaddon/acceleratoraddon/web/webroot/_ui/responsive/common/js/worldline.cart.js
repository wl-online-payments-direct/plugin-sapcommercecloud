ACC.worldlineCart = {
    _autoload: [
        "replenishmentInit",
        "bindScheduleReplenishment",
	],
    bindScheduleReplenishment: function (data) {
        var form = $('#replenishmentForm');
        var placeReplenishment = false;
        var formContainer = $(".replenishment-form-container");
        var replenishOrderCheckbox = $("#replenishmentForm #replenishmentOrder");
        ACC.worldlineCart.showHideAdditionalFormParameters();
        $(replenishOrderCheckbox).change(function(e) {
            e.preventDefault();
            if (replenishOrderCheckbox.is(':checked')) {
                formContainer.prop("style", "display: block");
            } else {
                formContainer.prop("style", "display: none");
            }
        });
        $("#frequency").bind("load change",function (event) {
            event.stopPropagation();
            ACC.worldlineCart.showHideAdditionalFormParameters();
        });

        $(document).on("click", '#replenishmentSchedule .js-open-datepicker', function () {
            $(this).datepicker('show');
        });

        $(document).on("change", '#replenishmentStartDate', function (e) {
            ACC.worldlineCart.toggleReplenishmentScheduleStartDateError(false);
            ACC.worldlineCart.toggleReplenishmentScheduleWrongDatesError(false);
        });

        $(document).on("change", '#replenishmentEndDate', function (e) {
            ACC.worldlineCart.toggleReplenishmentScheduleEndDateError(false);
            ACC.worldlineCart.toggleReplenishmentScheduleWrongDatesError(false);
        });
    },
    validateDate: function (date, dateFormat) {
        var validDate = true;
        try {
            $.datepicker.parseDate(dateFormat, date);
        } catch (err) {
            //Bad date detected
            validDate = false;
        }
        return validDate;
    },
    getDate: function(date, format) {
        try {
            return $.datepicker.parseDate(format, date);
        } catch (err) {
            return null;
        }

    },
    validateDates: function () {
        var localeDateFormat = $('#replenishmentSchedule').data('dateForDatePicker');
        var startDateEntered = $("#replenishmentStartDate").val();
        var endDateEntered = $("#replenishmentEndDate").val();
        let startDate = ACC.worldlineCart.getDate(startDateEntered, localeDateFormat);
        let endDate = ACC.worldlineCart.getDate(endDateEntered, localeDateFormat);
        return (startDate!= null && (endDate== null || startDate<endDate));
    },

    toggleReplenishmentScheduleStartDateError: function (showError) {
        if (showError) {
            var datePickerElem = $('#replenishmentSchedule .datepicker.start');
            if (!datePickerElem.hasClass('has-error')) {
                datePickerElem.addClass('has-error');
            }
            $('#errorReplenishmentStartDate').show();
        } else {
            $('#replenishmentSchedule .datepicker.start').removeClass('has-error');
            $('#errorReplenishmentStartDate').hide();
        }
    },
    toggleReplenishmentScheduleEndDateError: function (showError) {
        if (showError) {
            var datePickerElem = $('#replenishmentSchedule .datepicker.end');
            if (!datePickerElem.hasClass('has-error')) {
                datePickerElem.addClass('has-error');
            }
            $('#errorReplenishmentEndDate').show();
        } else {
            $('#replenishmentSchedule .datepicker.end').removeClass('has-error');
            $('#errorReplenishmentEndDate').hide();
        }
    },
    toggleReplenishmentScheduleWrongDatesError: function (showError) {
        if (showError) {
            var datePickerElem = $('#replenishmentSchedule .datepicker');
            datePickerElem.each(function () {
                if (!$(this).hasClass('has-error')) {
                    $(this).addClass('has-error');
                }
            })

            $('#errorDatesValidity').show();
        } else {
            $('#replenishmentSchedule .datepicker.start').removeClass('has-error');
            $('#replenishmentSchedule .datepicker.end').removeClass('has-error');
            $('#errorDatesValidity').hide();
        }
    },

    showHideAdditionalFormParameters: function() {
        switch ($("#frequency").val()) {
            case "DAILY":
                $('.scheduleformD').show();
                $('.scheduleformW').hide();
                $('.scheduleformM').hide();
                break;
            case "WEEKLY":
                $('.scheduleformD').hide();
                $('.scheduleformW').show();
                $('.scheduleformM').hide();
                break;
            case "MONTHLY":
                $('.scheduleformD').hide();
                $('.scheduleformW').hide();
                $('.scheduleformM').show();
                break
            default :
                $('.scheduleformD').hide();
                $('.scheduleformW').hide();
                $('.scheduleformM').hide();
                break;
        }
    },

    replenishmentInit: function () {
        var placeOrderFormReplenishmentOrder = $('#replenishmentSchedule').data("placeOrderFormReplenishmentOrder");
        var placeOrderFormReplenishmentRecurrence = $('#replenishmentSchedule').data("placeOrderFormReplenishmentRecurrence");
        var dateForDatePicker = $('#replenishmentSchedule').data("dateForDatePicker");
        var placeOrderFormNDays = $('#replenishmentSchedule').data("placeOrderFormNDays");
        var placeOrderFormNthDayOfMonth = $('#replenishmentSchedule').data("placeOrderFormNthDayOfMonth");

        if (placeOrderFormReplenishmentOrder === undefined) {
            return;
        }


        // replenishment schedule data not set to cart yet
        if (!placeOrderFormReplenishmentOrder) {

            $("#frequency option[value=" + placeOrderFormReplenishmentRecurrence + "]").attr('selected', 'selected');

            $("#nDays option[value=" + placeOrderFormNDays + "]").attr('selected', 'selected');

            // default value for weekly
            //$("input:radio[name='replenishmentRecurrence'][value=WEEKLY]").prop('checked', false);
            $('.scheduleformW').hide();

            // default value for monthly
            //$("input:radio[name='replenishmentRecurrence'][value=MONTHLY]").prop('checked', false);
            $('.scheduleformM').hide();

            // default value for yearly
            //$("input:radio[name='replenishmentRecurrence'][value=YEARLY]").prop('checked', false);
            $('.scheduleformY').hide();

            if (placeOrderFormNthDayOfMonth != '')
                $("#nthDayOfMonth option[value=" + placeOrderFormNthDayOfMonth + "]").attr('selected', 'selected');

            $("#replenishmentStartDate").val("");
            $("#replenishmentEndDate").val("");
        } else {
            switch (placeOrderFormReplenishmentRecurrence) {
                case "DAILY":
                    $('.scheduleformD').show();
                    break;
                case "WEEKLY":
                    $('.scheduleformW').show();
                    break;
                case "MONTHLY":
                    $('.scheduleformM').show();
                    break
            }
        }
        var currentDate = new Date();
        currentDate.setDate(currentDate.getDate() + 1);
        $(".js-replenishment-datepicker").datepicker({
            dateFormat: dateForDatePicker,
            onClose: function () {
                if (!ACC.worldlineCart.validateDates())
                {
                    ACC.worldlineCart.toggleReplenishmentScheduleWrongDatesError(true);
                }
            },
            minDate: currentDate
        });

    }
}