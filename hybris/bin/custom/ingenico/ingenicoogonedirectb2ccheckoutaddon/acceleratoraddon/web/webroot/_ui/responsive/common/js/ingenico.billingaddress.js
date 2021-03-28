ACC.ingenicoOrderPost = {

    spinner: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif"),

    binduseIngenicoDeliveryAddress: function ()
    {
        $('#useIngenicoDeliveryAddress').on('change', function ()
        {
            if ($('#useIngenicoDeliveryAddress').is(":checked"))
            {
                var options = {'countryIsoCode': $('#useIngenicoDeliveryAddressData').data('countryisocode'), 'useDeliveryAddress': true};
                ACC.ingenicoOrderPost.enableAddressForm();
                ACC.ingenicoOrderPost.displayIngenicoAddressForm(options, ACC.ingenicoOrderPost.useIngenicoDeliveryAddressSelected);
                ACC.ingenicoOrderPost.disableAddressForm();
            }
            else
            {
                ACC.ingenicoOrderPost.clearAddressForm();
                ACC.ingenicoOrderPost.enableAddressForm();
            }
        });

        if ($('#useIngenicoDeliveryAddress').is(":checked"))
        {
            var options = {'countryIsoCode': $('#useIngenicoDeliveryAddressData').data('countryisocode'), 'useDeliveryAddress': true};
            ACC.ingenicoOrderPost.enableAddressForm();
            ACC.ingenicoOrderPost.displayIngenicoAddressForm(options, ACC.ingenicoOrderPost.useIngenicoDeliveryAddressSelected);
            ACC.ingenicoOrderPost.disableAddressForm();
        }
    },

    bindSubmitingenicoSelectPaymentForm: function ()
    {
        $('.submit_ingenicoSelectPaymentForm').click(function ()
        {
            ACC.common.blockFormAndShowProcessingMessage($(this));
            $('.ingenicoBillingAddressForm').filter(":hidden").remove();
            ACC.ingenicoOrderPost.enableAddressForm();
            $('#ingenicoSelectPaymentForm').submit();
        });
    },

    bindCycleFocusEvent: function ()
    {
        $('#lastInTheForm').blur(function ()
        {
            $('#ingenicoSelectPaymentForm [tabindex$="10"]').focus();
        })
    },

    isEmpty: function (obj)
    {
        if (typeof obj == 'undefined' || obj === null || obj === '') return true;
        return false;
    },

    disableAddressForm: function ()
    {
        $('input[id^="address\\."]').prop('disabled', true);
        $('select[id^="address\\."]').prop('disabled', true);
    },

    enableAddressForm: function ()
    {
        $('input[id^="address\\."]').prop('disabled', false);
        $('select[id^="address\\."]').prop('disabled', false);
    },

    clearAddressForm: function ()
    {
        $('input[id^="address\\."]').val("");
        $('select[id^="address\\."]').val("");
    },

    useIngenicoDeliveryAddressSelected: function ()
    {
        if ($('#useIngenicoDeliveryAddress').is(":checked"))
        {
            var countryIsoCode = $('#address\\.country').val($('#useIngenicoDeliveryAddressData').data('countryisocode')).val();
            if(ACC.ingenicoOrderPost.isEmpty(countryIsoCode))
            {
                $('#useIngenicoDeliveryAddress').click();
                $('#useIngenicoDeliveryAddress').parent().hide();
            }
            else
            {
                ACC.ingenicoOrderPost.disableAddressForm();
            }
        }
        else
        {
            ACC.ingenicoOrderPost.clearAddressForm();
            ACC.ingenicoOrderPost.enableAddressForm();
        }
    },

    displayIngenicoAddressForm: function (options, callback)
    {
        $.ajax({
            url: ACC.config.encodedContextPath + '/checkout/multi/ingenico/billingaddressform',
            async: true,
            data: options,
            dataType: "html",
            beforeSend: function ()
            {
                $('#ingenicoBillingAddressForm').html(ACC.ingenicoOrderPost.spinner);
            }
        }).done(function (data)
        {
            $("#ingenicoBillingAddressForm").html(data);
            if (typeof callback == 'function')
            {
                callback.call();
            }
        });
    }
}

$(document).ready(function ()
{
    ACC.ingenicoOrderPost.binduseIngenicoDeliveryAddress();
    ACC.ingenicoOrderPost.bindSubmitingenicoSelectPaymentForm();

    // check the checkbox
    $("#useIngenicoDeliveryAddress").click();
});
