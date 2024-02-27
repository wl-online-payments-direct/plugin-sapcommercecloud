const PAYMENT_METHOD_SELECTORS = {
    PLACE_ORDER_FORM: '.js-worldlinePlaceOrderForm',
    PAYMENT_METHOD_FORM: '#worldlineSelectPaymentForm',
    PAYMENT_METHOD_SUBMIT_BUTTON: '.submit_worldlineSelectPaymentForm',
    CARD_ROW_TABLE_ROW: '.card-row',
    PAYMENT_PRODUCT_ROW: '.js-worldline_payment_product',
    PAYMENT_PRODUCT_IDEAL_ROW: '.worldline_payment_product_detail',
    PAYMENT_METHOD_CONTAINER: '.worldline_payment_products',
    SAVED_CARD_CODE_PARAM: 'savedCardCode',
    TOKENIZATION_FORM_TABLE_ROW: '.js-hostedTokenization',
    TOKENIZATION_FORM_CLASS: 'js-hostedTokenization',
    HTP_CLASS: '.htp'
}
ACC.worldlinePaymentPost = {
    tokenizer: {},
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
                $(PAYMENT_METHOD_SELECTORS.PLACE_ORDER_FORM + ' input[name=' + key + ']').val(json[key]);
        }

    },

    bindSubmitWorldlineSelectPaymentForm: function () {
        $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_SUBMIT_BUTTON).click(function () {
            var isSavedCard = $('input:radio[name="paymentProductId"]:checked').attr("is-saved");
            var isHTP = $(PAYMENT_METHOD_SELECTORS.HTP_CLASS);
            if (isSavedCard === "true") {
                $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_FORM + ' input[name=' + PAYMENT_METHOD_SELECTORS.SAVED_CARD_CODE_PARAM + ']').val($('input:radio[name="paymentProductId"]:checked').attr("code"));
            } else {
                $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_FORM + ' input[name=' + PAYMENT_METHOD_SELECTORS.SAVED_CARD_CODE_PARAM + ']').val('');
            }
            if (isHTP.length && isSavedCard) {
                if (ACC.worldlinePaymentPost.tokenizer !== undefined) {
                    var submitBtn = $(this);
                    var form = submitBtn.parents('form:first');
                    form.block({message: ACC.worldlinePaymentPost.spinner});
                    ACC.worldlinePaymentPost.tokenizer.submitTokenization().then((result) => {
                        if (result.success) {
                            console.log("Tokenization succeed :", result.hostedTokenizationId);
                            $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_FORM + ' input[name=hostedTokenizationId]').val(result.hostedTokenizationId);

                            $('.worldlineBillingAddressForm').filter(":hidden").remove();
                            ACC.worldlineOrderPost.enableAddressForm();

                            $('#worldlineSelectPaymentForm').submit();
                            submitBtn.unbind("click");
                        } else {
                            $([document.documentElement, document.body]).animate({
                                scrollTop: $(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).offset().top
                            }, 500);
                            console.log("Tokenization failed :", result.error);
                        }
                    }).catch(function (error) {
                        console.error("Unknown Error :", error);
                    }).finally(function () {
                        form.unblock();
                    });
                } else {
                    console.error("tokenizer is undefined !!");
                }
            } else {
                ACC.common.blockFormAndShowProcessingMessage($(this));
                $('.worldlineBillingAddressForm').filter(":hidden").remove();
                ACC.worldlineOrderPost.enableAddressForm();
                $('#worldlineSelectPaymentForm').submit();
            }
        });
        $(PAYMENT_METHOD_SELECTORS.CARD_ROW_TABLE_ROW).click(function () {
            $(this).find('td input:radio').prop('checked', true).change();
        });
        $(PAYMENT_METHOD_SELECTORS.PAYMENT_PRODUCT_ROW).click(function () {
            $(this).find('input:radio').prop('checked', true).change();
        })
    },

    bindSelectWorldlinePaymentProduct: function () {
        var $paymentProduct = $(PAYMENT_METHOD_SELECTORS.PAYMENT_PRODUCT_ROW + ' .payment_product');
        $paymentProduct.on('change', function () {
            var paymentProduct = $(this);
            if (paymentProduct.is(':checked')) {
                $(PAYMENT_METHOD_SELECTORS.PAYMENT_PRODUCT_IDEAL_ROW).addClass('display-none');
                paymentProduct.siblings(PAYMENT_METHOD_SELECTORS.PAYMENT_PRODUCT_IDEAL_ROW).removeClass('display-none');
            }
        });
        $paymentProduct.each(function () {
            if ($(this).is(':checked')) {
                $(this).siblings(PAYMENT_METHOD_SELECTORS.PAYMENT_PRODUCT_IDEAL_ROW).removeClass('display-none');
            }
        });
    },

    bindSubmitWorldlinePlaceOrderForm: function () {
        $(PAYMENT_METHOD_SELECTORS.PLACE_ORDER_FORM).submit(function (e) {
            ACC.worldlinePaymentPost.fillInternalDataWorldlinePlaceOrderForm();
            return true;
        });
    },

    bindWorldlineSavedPayments: function () {


        $('input:radio[name="paymentProductId"]').change(function () {
            var isHTP = $(PAYMENT_METHOD_SELECTORS.HTP_CLASS);

            if ($(this).attr("is-saved") && isHTP.length) {
                $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).block({message: ACC.worldlinePaymentPost.spinner});
                let token = $(this).attr("token");
                if ($(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).length > 0) {
                    ACC.worldlinePaymentPost.tokenizer.destroy();
                    $(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).addClass("display-none").removeClass(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_CLASS);
                }
                let tokenizationForm = $(this).closest('tr').next('tr').removeClass('display-none').addClass(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_CLASS).find(".hostedTokenization");
                var params = {
                    validationCallback: validateHostedTokenizationSubmit,
                    hideCardholderName: false,
                    hideTokenFields: false
                };
                ACC.worldlinePaymentPost.waitForElm(tokenizationForm.attr("id"))
                ACC.worldlinePaymentPost.tokenizer = new Tokenizer($(".js-hostedTokenization-partialRedirectUrl").attr("value"), tokenizationForm.attr("name"), params);
                ACC.worldlinePaymentPost.tokenizer.initialize()
                    .then(() => {
                        if (token) {
                            ACC.worldlinePaymentPost.tokenizer.useToken(token);
                        }
                        $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).unblock();
                    })
                    .catch(reason => {
                        console.log("tokenizer", "error on init");
                        $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).unblock();
                    });
            } else {
                if ($(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).length > 0) {
                    ACC.worldlinePaymentPost.tokenizer.destroy();
                    $(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).addClass("display-none").removeClass(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_CLASS);
                }
            }
        });
    },
    checkApplePayAvailability: function () {
        if (window.ApplePaySession) {
            if (ApplePaySession.canMakePayments()) {
                $('#worldline_payment_product_302').removeClass("display-none");
                return;
            }
        }
        $('#worldline_payment_product_302').remove();
    }, load: function () {
        let $selectedRadio = $('input:radio[name="paymentProductId"]:checked');
        var isHTP = $(PAYMENT_METHOD_SELECTORS.HTP_CLASS);

        if ($selectedRadio.attr("is-saved") !== undefined && $selectedRadio.attr("is-saved").length && isHTP.length) {
            $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).block({message: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif")});
            let token = $selectedRadio.attr("token");
            let tokenizationForm = $selectedRadio.closest('tr').next('tr').removeClass('display-none').addClass('js-hostedTokenization').find(".hostedTokenization");
            var params = {
                validationCallback: validateHostedTokenizationSubmit,
                hideCardholderName: false,
                hideTokenFields: false
            };

            ACC.worldlinePaymentPost.waitForElm(tokenizationForm.attr("id"))
            ACC.worldlinePaymentPost.tokenizer = new Tokenizer($(".js-hostedTokenization-partialRedirectUrl").attr("value"), tokenizationForm.attr("name"), params);
            ACC.worldlinePaymentPost.tokenizer.initialize()
                .then(() => {
                    if (token) {
                        ACC.worldlinePaymentPost.tokenizer.useToken(token);
                    }
                    $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).unblock();
                })
                .catch(reason => {
                    console.log("tokenizer", "error on init");
                    $(PAYMENT_METHOD_SELECTORS.PAYMENT_METHOD_CONTAINER).unblock();
                });
        } else {
            if ($(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).length > 0) {
                ACC.worldlinePaymentPost.tokenizer.destroy();
                $(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_TABLE_ROW).addClass("display-none").removeClass(PAYMENT_METHOD_SELECTORS.TOKENIZATION_FORM_CLASS);

            }
        }
    }, waitForElm: function (selector) {
        return new Promise(resolve => {
            if (document.querySelector(selector)) {
                return resolve(document.querySelector(selector));
            }
            const observer = new MutationObserver(mutations => {
                if (document.querySelector(selector)) {
                    resolve(document.querySelector(selector));
                    observer.disconnect();
                }
            });
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        });
    }
}

$(document).ready(function () {
    ACC.worldlinePaymentPost.bindSubmitWorldlineSelectPaymentForm();
    ACC.worldlinePaymentPost.bindSelectWorldlinePaymentProduct();
    ACC.worldlinePaymentPost.bindSubmitWorldlinePlaceOrderForm();
    ACC.worldlinePaymentPost.bindWorldlineSavedPayments();
    ACC.worldlinePaymentPost.checkApplePayAvailability();
    ACC.worldlinePaymentPost.load();
});

function validateHostedTokenizationSubmit(result) {
    console.log("tokenizer", result);
}
