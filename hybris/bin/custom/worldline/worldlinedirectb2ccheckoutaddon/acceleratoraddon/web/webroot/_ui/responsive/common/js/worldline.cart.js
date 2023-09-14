ACC.worldlineCart = {
    _autoload: [
        "bindScheduleReplenishment",
        "replenishmentInit"
	],
    bindScheduleReplenishment: function (data) {
        var form = $('#replenishmentForm');
        var placeReplenishment = false;
        var formContainer = $(".replenishment-form-container");
        var replenishOrderCheckbox = $("#replenishmentForm #replenishmentOrder");
        $(document).on("change", replenishOrderCheckbox, function(e) {
            e.preventDefault();
            if (replenishOrderCheckbox.is(':checked')) {
                formContainer.prop("style", "display: block");
                $("input:radio[name=replenishmentRecurrence]").click(function () {
//                    if ($("#replenishmentStartDate").val() != '' && ACC.worldlineCart.validateDates()) {
//                        $('#replenishmentSchedule .js-replenishment-actions').show();
//                    }
                    switch (this.value) {
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
                });
            } else {
                formContainer.prop("style", "display: none");
            }

        })

        $(document).on("click", '#replenishmentSchedule .js-open-datepicker', function () {
            console.log($(this));
            $(this).datepicker('show');
            console.log("event added")
        });

//        $(document).on("click", '#replenishmentSchedule #placeReplenishmentOrder', function (e) {
//            e.preventDefault();
//
//            var localeDateFormat = $('#replenishmentSchedule').data('dateForDatePicker');
//            var startDateEntered = $("#replenishmentStartDate").val();
//            var endDateEntered = $("#replenishmentEndDate").val();
//            let validateStartDate = ACC.checkoutsummary.validateDate(startDateEntered, localeDateFormat);
//            let validateEndDate = ACC.checkoutsummary.validateDate(endDateEntered, localeDateFormat);
//            if (validateStartDate  &&validateEndDate) {
//                $(".replenishmentOrderClass").val(true);
//                placeReplenishment = true;
//                $.colorbox.close();
//            } else
//            {
//                if (!validateStartDate)
//                {
//                    ACC.checkoutsummary.toggleReplenishmentScheduleStartDateError(true);
//                }
//                if (!validateEndDate)
//                {
//                    ACC.checkoutsummary.toggleReplenishmentScheduleEndDateError(true);
//                }
//                $.colorbox.resize();
//
//            }
//
//
//        });

        $(document).on("change", '#replenishmentStartDate', function (e) {
            ACC.checkoutsummary.toggleReplenishmentScheduleStartDateError(false);
        });

        $(document).on("change", '#replenishmentEndDate', function (e) {
            ACC.checkoutsummary.toggleReplenishmentScheduleEndDateError(false);
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

            //$('#replenishmentSchedule .js-replenishment-actions').hide();

            // default value for daily
            $("input:radio[name='replenishmentRecurrence'][value=DAILY]").prop('checked', false);
            $('.scheduleformD').hide();
            $("#nDays option[value=" + placeOrderFormNDays + "]").attr('selected', 'selected');

            // default value for weekly
            $("input:radio[name='replenishmentRecurrence'][value=WEEKLY]").prop('checked', false);
            $('.scheduleformW').hide();

            // default value for monthly
            $("input:radio[name='replenishmentRecurrence'][value=MONTHLY]").prop('checked', false);
            $('.scheduleformM').hide();

            // default value for yearly
            $("input:radio[name='replenishmentRecurrence'][value=YEARLY]").prop('checked', false);
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

        $(".js-replenishment-datepicker").datepicker({
            dateFormat: dateForDatePicker,
            onClose: function () {
                if (!ACC.worldlineCart.validateDates())
                {
                    console.log("Do something if the dates are not valid")
                } else {
                    if ($("input:radio[name=replenishmentRecurrence]").is(':checked')) {
                        console.log("Do something if the dates are valid")
                    }
                }

            }
        });

    }
}