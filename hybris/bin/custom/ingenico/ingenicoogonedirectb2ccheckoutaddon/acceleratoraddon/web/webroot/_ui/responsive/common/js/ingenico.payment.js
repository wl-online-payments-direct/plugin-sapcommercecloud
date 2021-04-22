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
                var submitBtn =$(this);
                var form = submitBtn.parents('form:first');
                form.block({message: ACC.ingenicoPaymentPost.spinner});
                tokenizer.submitTokenization().then((result) => {
                    if (result.success) {
                        console.log("Tokenization succeed :", result.hostedTokenizationId);
                        $('#ingenicoDoPaymentForm  input[name=hostedTokenizationId]').val(result.hostedTokenizationId);
                        $('#ingenicoDoPaymentForm').submit();
                        submitBtn.unbind("click");
                    } else {
                        console.log("Tokenization failed :", result.error);
                    }
                }).catch(function (error) {
                    console.error("Unknown Error :", error);
                }).finally(function () {
                    form.unblock();
                });


            }
        });
    },
    bindSavedIngenicoCardSelect: function () {
        $("#select_payment-tokens").change(function () {
            if (tokenizer !== undefined) {
                var useSavedCard = $(this).val();
                if (useSavedCard === "") {
                    tokenizer.useToken();
                } else {
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
