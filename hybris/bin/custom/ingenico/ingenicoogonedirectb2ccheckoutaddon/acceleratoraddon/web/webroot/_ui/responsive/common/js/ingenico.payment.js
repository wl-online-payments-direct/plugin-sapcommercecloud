ACC.ingenicoPaymentPost = {

    spinner: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif"),
    internalData: function () {
        return {
            "colorDepth": screen.colorDepth,
            "screenHeight": screen.height,
            "screenWidth": screen.width,
            "navigatorJavaEnabled": navigator.javaEnabled(),
            "navigatorJavaScriptEnabled": true,
            "timezoneOffset": new Date().getTimezoneOffset()
        }
    },

    fillInternalDataIngenicoPlaceOrderForm: function () {
        var json = ACC.ingenicoPaymentPost.internalData();
        for (key in json) {
            if (json.hasOwnProperty(key))
                $('#ingenicoPlaceOrderForm  input[name=' + key + ']').val(json[key]);
        }

    },

    bindSubmitIngenicoSelectPaymentForm: function () {
        $('.submit_ingenicoSelectPaymentForm').click(function () {
            var isHTP = $('.ingenico_payment_product .htp').is(':checked');
            if (isHTP) {
                if (tokenizer !== undefined) {
                    var submitBtn = $(this);
                    var form = submitBtn.parents('form:first');
                    form.block({message: ACC.ingenicoPaymentPost.spinner});
                    tokenizer.submitTokenization().then((result) => {
                        if (result.success) {
                            console.log("Tokenization succeed :", result.hostedTokenizationId);
                            $('#ingenicoSelectPaymentForm  input[name=hostedTokenizationId]').val(result.hostedTokenizationId);

                            $('.ingenicoBillingAddressForm').filter(":hidden").remove();
                            ACC.ingenicoOrderPost.enableAddressForm();

                            $('#ingenicoSelectPaymentForm').submit();
                            submitBtn.unbind("click");
                        } else {
                            console.log("Tokenization failed :", result.error);
                        }
                    }).catch(function (error) {
                        console.error("Unknown Error :", error);
                    }).finally(function () {
                        form.unblock();
                    });
                }else{
                    console.error("tokenizer is undefined !!");
                }
            } else {
                ACC.common.blockFormAndShowProcessingMessage($(this));
                $('.ingenicoBillingAddressForm').filter(":hidden").remove();
                ACC.ingenicoOrderPost.enableAddressForm();
                $('#ingenicoSelectPaymentForm').submit();
            }
        });
    },

    bindSelectIngenicoPaymentProduct: function () {
        var $paymentProduct = $('.ingenico_payment_product .payment_product');
        $paymentProduct.on('change', function () {
            var paymentProduct = $(this);
            if (paymentProduct.is(':checked')) {
                $('.ingenico_payment_product_detail').addClass('display-none');
                paymentProduct.siblings('.ingenico_payment_product_detail').removeClass('display-none');
            }
        });
        $paymentProduct.each(function () {
            if ($(this).is(':checked')) {
                $(this).siblings('.ingenico_payment_product_detail').removeClass('display-none');
            }
        });
    },

    bindSubmitIngenicoPlaceOrderForm: function () {
        $('#ingenicoPlaceOrderForm').submit(function (e) {
            ACC.ingenicoPaymentPost.fillInternalDataIngenicoPlaceOrderForm();
            return true;
        });
    },

    bindIngenicoSavedPayments: function () {
        $(document).on("click", ".js-saved-ingenico-payments", function (e) {
            e.preventDefault();

            var title = $("#savedpaymentstitle").html();

            $.colorbox({
                href: "#savedpaymentsbody",
                inline: true,
                maxWidth: "100%",
                opacity: 0.7,
                title: title,
                close: '<span class="glyphicon glyphicon-remove"></span>',
                onComplete: function () {
                }
            });
        });
        $(document).on("click", ".js-use-saved-ingenico-payment", function (e) {
            e.preventDefault();
            if (tokenizer !== undefined) {
                tokenizer.useToken($(this).data('token'));
                $(".js-saved-ingenico-payments").parent().toggleClass("display-none");
                $(".js-reset-token-form").parent().toggleClass("display-none");
                $.colorbox.close();
            }
        });
        $(document).on("click", ".js-reset-token-form", function (e) {
            e.preventDefault();
            if (tokenizer !== undefined) {
                tokenizer.useToken();
                $(".js-saved-ingenico-payments").parent().toggleClass("display-none");
                $(".js-reset-token-form").parent().toggleClass("display-none");
            }
        });
    }
}

$(document).ready(function () {
    ACC.ingenicoPaymentPost.bindSubmitIngenicoSelectPaymentForm();
    ACC.ingenicoPaymentPost.bindSelectIngenicoPaymentProduct();
    ACC.ingenicoPaymentPost.bindSubmitIngenicoPlaceOrderForm();
    ACC.ingenicoPaymentPost.bindIngenicoSavedPayments();
});
