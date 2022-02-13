ACC.worldlinePaymentPost = {

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

    fillInternalDataWorldlinePlaceOrderForm: function () {
        var json = ACC.worldlinePaymentPost.internalData();
        for (key in json) {
            if (json.hasOwnProperty(key))
                $('#worldlinePlaceOrderForm  input[name=' + key + ']').val(json[key]);
        }

    },

    bindSubmitWorldlineSelectPaymentForm: function () {
        $('.submit_worldlineSelectPaymentForm').click(function () {
            var isHTP = $('.worldline_payment_product .htp').is(':checked');
            if (isHTP) {
                if (tokenizer !== undefined) {
                    var submitBtn = $(this);
                    var form = submitBtn.parents('form:first');
                    form.block({message: ACC.worldlinePaymentPost.spinner});
                    tokenizer.submitTokenization().then((result) => {
                        if (result.success) {
                            console.log("Tokenization succeed :", result.hostedTokenizationId);
                            $('#worldlineSelectPaymentForm  input[name=hostedTokenizationId]').val(result.hostedTokenizationId);

                            $('.worldlineBillingAddressForm').filter(":hidden").remove();
                            ACC.worldlineOrderPost.enableAddressForm();

                            $('#worldlineSelectPaymentForm').submit();
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
                $('.worldlineBillingAddressForm').filter(":hidden").remove();
                ACC.worldlineOrderPost.enableAddressForm();
                $('#worldlineSelectPaymentForm').submit();
            }
        });
    },

    bindSelectWorldlinePaymentProduct: function () {
        var $paymentProduct = $('.worldline_payment_product .payment_product');
        $paymentProduct.on('change', function () {
            var paymentProduct = $(this);
            if (paymentProduct.is(':checked')) {
                $('.worldline_payment_product_detail').addClass('display-none');
                paymentProduct.siblings('.worldline_payment_product_detail').removeClass('display-none');
            }
        });
        $paymentProduct.each(function () {
            if ($(this).is(':checked')) {
                $(this).siblings('.worldline_payment_product_detail').removeClass('display-none');
            }
        });
    },

    bindSubmitWorldlinePlaceOrderForm: function () {
        $('#worldlinePlaceOrderForm').submit(function (e) {
            ACC.worldlinePaymentPost.fillInternalDataWorldlinePlaceOrderForm();
            return true;
        });
    },

    bindWorldlineSavedPayments: function () {
        $(document).on("click", ".js-saved-worldline-payments", function (e) {
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
        $(document).on("click", ".js-use-saved-worldline-payment", function (e) {
            e.preventDefault();
            if (tokenizer !== undefined) {
                tokenizer.useToken($(this).data('token'));
                $(".js-saved-worldline-payments").parent().toggleClass("display-none");
                $(".js-reset-token-form").parent().toggleClass("display-none");
                $.colorbox.close();
            }
        });
        $(document).on("click", ".js-reset-token-form", function (e) {
            e.preventDefault();
            if (tokenizer !== undefined) {
                tokenizer.useToken();
                $(".js-saved-worldline-payments").parent().toggleClass("display-none");
                $(".js-reset-token-form").parent().toggleClass("display-none");
            }
        });
    }
}

$(document).ready(function () {
    ACC.worldlinePaymentPost.bindSubmitWorldlineSelectPaymentForm();
    ACC.worldlinePaymentPost.bindSelectWorldlinePaymentProduct();
    ACC.worldlinePaymentPost.bindSubmitWorldlinePlaceOrderForm();
    ACC.worldlinePaymentPost.bindWorldlineSavedPayments();
});
