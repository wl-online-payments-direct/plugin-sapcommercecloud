ACC.ingenicoPaymentPost = {

    spinner: $("<img>").attr("src", ACC.config.commonResourcePath + "/images/spinner.gif"),
    internalData: function() {
        return {
            "colorDepth": screen.colorDepth,
            "screenHeight": screen.height,
            "screenWidth": screen.width,
            "navigatorJavaEnabled": navigator.javaEnabled(),
            "navigatorJavaScriptEnabled": true,
            "timezoneOffset": new Date().getTimezoneOffset()
        }
    },
    fillInternalDataIngenicoDoPaymentForm: function () {
        var json = ACC.ingenicoPaymentPost.internalData();
        for (key in json) {
            if (json.hasOwnProperty(key))
                $('#ingenicoDoPaymentForm  input[name=' + key + ']').val(json[key]);
        }

    },
    fillInternalDataIngenicoPlaceOrderForm: function () {
        var json = ACC.ingenicoPaymentPost.internalData();
        for (key in json) {
            if (json.hasOwnProperty(key))
                $('#ingenicoPlaceOrderForm  input[name=' + key + ']').val(json[key]);
        }

    },
    bindSubmitingenicoDoPaymentForm: function () {
        $('.submit_ingenicoDoPaymentForm').click(function () {
            ACC.ingenicoPaymentPost.fillInternalDataIngenicoDoPaymentForm();
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
    bindSubmitIngenicoPlaceOrderForm: function () {
        $('#ingenicoPlaceOrderForm').submit(function (e) {
            ACC.ingenicoPaymentPost.fillInternalDataIngenicoPlaceOrderForm();
            return true;
        });
    },
    bindIngenicoSavedPayments:function(){
        $(document).on("click",".js-saved-ingenico-payments",function(e){
            e.preventDefault();

            var title = $("#savedpaymentstitle").html();

            $.colorbox({
                href: "#savedpaymentsbody",
                inline:true,
                maxWidth:"100%",
                opacity:0.7,
                title: title,
                close:'<span class="glyphicon glyphicon-remove"></span>',
                onComplete: function(){
                }
            });
        });
        $(document).on("click",".js-use-saved-ingenico-payment",function(e){
            e.preventDefault();
            if (tokenizer !== undefined) {
                tokenizer.useToken($(this).data('token'));
                $(".js-saved-ingenico-payments").parent().toggleClass("display-none");
                $(".js-reset-token-form").parent().toggleClass("display-none");
                $.colorbox.close();
            }
        });
        $(document).on("click",".js-reset-token-form",function(e){
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
    ACC.ingenicoPaymentPost.bindSubmitingenicoDoPaymentForm();
    ACC.ingenicoPaymentPost.bindSubmitIngenicoPlaceOrderForm();
    ACC.ingenicoPaymentPost.bindIngenicoSavedPayments();
});
