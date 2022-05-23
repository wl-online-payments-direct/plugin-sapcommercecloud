const selectors = {
    BTN_SELECT: '.btn-select',
    SELECT_TOGGLE_CONTAINER: '.select-toggle-container',
    WORLDLINE_SELECTOR: '#worldline-selector',
    TOKEN_ATTRIBUTE: 'token',
    WORLDLINE_SELECT_PAYMENT_FORM: '#worldlineSelectPaymentForm',
    SUBMIT_TOKEN_NAME: 'hostedCheckoutToken'
}
ACC.worldlinePaymentMode = {

    init: function () {
        $(selectors.BTN_SELECT).html($(selectors.WORLDLINE_SELECTOR + ' li[selected]').first().clone());
        $(selectors.WORLDLINE_SELECT_PAYMENT_FORM + ' input[name=' + selectors.SUBMIT_TOKEN_NAME + ']').val($(selectors.WORLDLINE_SELECTOR + ' li[selected]').first().attr(selectors.TOKEN_ATTRIBUTE));
    },
    bindBtnSubmitClick: function () {
        $(selectors.BTN_SELECT).click(function () {
            $(selectors.SELECT_TOGGLE_CONTAINER).toggle();

        });
    },
    onItemClick: function () {
        $(selectors.WORLDLINE_SELECTOR + ' li').click(function () {
            $(selectors.BTN_SELECT).empty();
            $(this).clone().appendTo(selectors.BTN_SELECT);
            $(selectors.WORLDLINE_SELECT_PAYMENT_FORM + ' input[name=' + selectors.SUBMIT_TOKEN_NAME + ']').val($(this).attr(selectors.TOKEN_ATTRIBUTE));

            $(selectors.BTN_SELECT).attr('value', '');
            $(selectors.SELECT_TOGGLE_CONTAINER).toggle();
        });

    }
}
$(document).ready(function () {
    ACC.worldlinePaymentMode.init();
    ACC.worldlinePaymentMode.bindBtnSubmitClick();
    ACC.worldlinePaymentMode.onItemClick();
});