ACC.ingenicoPaymentPost = {

    spinner: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif"),
    internalData: {
        "colorDepth": screen.colorDepth,
        "screenHeight": screen.height,
        "screenWidth": screen.width,
        "navigatorJavaEnabled": navigator.javaEnabled(),
        "timezoneOffset": new Date().getTimezoneOffset()
    },
    fillInternalDataIngenicoDoPaymentForm: function () {
        var json = ACC.ingenicoPaymentPost.internalData;
        for (key in json) {
            if (json.hasOwnProperty(key))
                $('#ingenicoDoPaymentForm  input[name=' + key + ']').val(json[key]);
        }

    },
    bindSubmitingenicoDoPaymentForm: function () {
        $('.submit_ingenicoDoPaymentForm').click(function () {
            if (tokenizer !== undefined) {
                ACC.common.blockFormAndShowProcessingMessage($(this));

                tokenizer.submitTokenization().then((result) => {
                    if (result.success) {
                        console.log("success Tokenization :", result.hostedTokenizationId);
                        $('#ingenicoDoPaymentForm  input[name=hostedTokenizationId]').val(result.hostedTokenizationId);
                        $('#ingenicoDoPaymentForm').submit();
                    } else {
                        console.log("error Tokenization :", result.error);
                    }
                });


            }
        });
    },
    bindSavedIngenicoCardSelect: function () {
        $("#select_payment-tokens").change(function () {
            if (tokenizer !== undefined) {
                var useSavedCard = $(this).val();
                if(useSavedCard===""){
                    tokenizer.useToken();
                }else {
                    tokenizer.useToken(useSavedCard);
                }
            }
        });
    }
}

$(document).ready(function () {
    ACC.ingenicoPaymentPost.bindSubmitingenicoDoPaymentForm();
    ACC.ingenicoPaymentPost.fillInternalDataIngenicoDoPaymentForm();
    ACC.ingenicoPaymentPost.bindSavedIngenicoCardSelect();
});
