ACC.worldlineOrderPost = {

    spinner: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif"),

    binduseWorldlineDeliveryAddress: function () {
        $('#useWorldlineDeliveryAddress').on('change', function () {
            if ($('#useWorldlineDeliveryAddress').is(":checked")) {
                var options = {
                    'countryIsoCode': $('#useWorldlineDeliveryAddressData').data('countryisocode'),
                    'useDeliveryAddress': true
                };
                ACC.worldlineOrderPost.enableAddressForm();
                ACC.worldlineOrderPost.displayWorldlineAddressForm(options, ACC.worldlineOrderPost.useWorldlineDeliveryAddressSelected);
                ACC.worldlineOrderPost.disableAddressForm();
            } else {
                ACC.worldlineOrderPost.clearAddressForm();
                ACC.worldlineOrderPost.enableAddressForm();
            }
        });

        if ($('#useWorldlineDeliveryAddress').is(":checked")) {
            var options = {
                'countryIsoCode': $('#useWorldlineDeliveryAddressData').data('countryisocode'),
                'useDeliveryAddress': true
            };
            ACC.worldlineOrderPost.enableAddressForm();
            ACC.worldlineOrderPost.displayWorldlineAddressForm(options, ACC.worldlineOrderPost.useWorldlineDeliveryAddressSelected);
            ACC.worldlineOrderPost.disableAddressForm();
        }
    },

    bindCycleFocusEvent: function () {
        $('#lastInTheForm').blur(function () {
            $('#worldlineSelectPaymentForm [tabindex$="10"]').focus();
        })
    },

    isEmpty: function (obj) {
        if (typeof obj == 'undefined' || obj === null || obj === '') return true;
        return false;
    },

    disableAddressForm: function () {
        $('input[id^="address\\."]').prop('disabled', true);
        $('select[id^="address\\."]').prop('disabled', true);
    },

    enableAddressForm: function () {
        $('input[id^="address\\."]').prop('disabled', false);
        $('select[id^="address\\."]').prop('disabled', false);
    },

    clearAddressForm: function () {
        $('input[id^="address\\."]').val("");
        $('select[id^="address\\."]').val("");
    },

    useWorldlineDeliveryAddressSelected: function () {
        if ($('#useWorldlineDeliveryAddress').is(":checked")) {
            var countryIsoCode = $('#address\\.country').val($('#useWorldlineDeliveryAddressData').data('countryisocode')).val();
            if (ACC.worldlineOrderPost.isEmpty(countryIsoCode)) {
                $('#useWorldlineDeliveryAddress').click();
                $('#useWorldlineDeliveryAddress').parent().hide();
            } else {
                ACC.worldlineOrderPost.disableAddressForm();
            }
        } else {
            ACC.worldlineOrderPost.clearAddressForm();
            ACC.worldlineOrderPost.enableAddressForm();
        }
    },

    displayWorldlineAddressForm: function (options, callback) {
        $.ajax({
            url: ACC.config.encodedContextPath + '/checkout/multi/worldline/billingaddressform',
            async: true,
            data: options,
            dataType: "html",
            beforeSend: function () {
                $('#worldlineBillingAddressForm').html(ACC.worldlineOrderPost.spinner);
            }
        }).done(function (data) {
            $("#worldlineBillingAddressForm").html(data);
            if (typeof callback == 'function') {
                callback.call();
            }
        });
    }
}

$(document).ready(function () {
    ACC.worldlineOrderPost.binduseWorldlineDeliveryAddress();
    // check the checkbox
    $("#useWorldlineDeliveryAddress").click();
});
