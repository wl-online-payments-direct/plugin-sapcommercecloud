ACC.checkoutsummary = {

    _autoload: [
        "bindAllButtons"
    ],

    bindAllButtons: function () {
        ACC.checkoutsummary.toggleActionButtons('.place-order-form');

    },

    toggleActionButtons: function (selector) {

        var cssClass = $(document).find(selector);
        var checkoutBtns = cssClass.find('.checkoutSummaryButton');
         var checkBox = cssClass.find('input[name=termsCheck]');

        checkoutBtns.each(function () {$( this ).prop('disabled', true)});
        checkBox.on('click', function () {
            var checked = $(this).prop('checked');

            if (checked) {
                checkoutBtns.each(function () {$( this ).prop('disabled', false)});
            } else {
                checkoutBtns.each(function () {$( this ).prop('disabled', true)});
            }
        });
    }

};
