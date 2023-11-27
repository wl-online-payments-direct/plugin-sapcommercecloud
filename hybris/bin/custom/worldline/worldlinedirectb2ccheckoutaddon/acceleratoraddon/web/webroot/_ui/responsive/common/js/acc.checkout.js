ACC.checkout = {

	_autoload: [
		"bindCheckO",
		"bindForms",
		"bindSavedPayments",
		"bindPlaceOrder"
	],

	selectPciOption: "#selectPciOption",


	bindForms:function(){

		$(document).on("click","#addressSubmit",function(e){
			e.preventDefault();
			$('#addressForm').submit();	
		})
		
		$(document).on("click","#deliveryMethodSubmit",function(e){
			e.preventDefault();
			$('#selectDeliveryMethodForm').submit();	
		})

	},

	bindSavedPayments:function(){
		$(document).on("click",".js-saved-payments",function(e){
			e.preventDefault();

			var title = $("#savedpaymentstitle").html();

			$.colorbox({
				href: "#savedpaymentsbody",
				inline:true,
				maxWidth:"100%",
				opacity:0.7,
				//width:"320px",
				title: title,
				close:'<span class="glyphicon glyphicon-remove"></span>',
				onComplete: function(){
				// This is intentional
				}
			});
		})
	},

	bindCheckO: function ()
	{
		var cartEntriesError = false;
		
		// Alternative checkout flows options
		$('.doFlowSelectedChange').change(function ()
		{
			if ('multistep-pci' == $('#selectAltCheckoutFlow').val())
			{
				$(ACC.checkout.selectPciOption).show();
			}
			else
			{
				$(ACC.checkout.selectPciOption).hide();

			}
		});



		$('.js-continue-shopping-button').click(function ()
		{
			var checkoutUrl = $(this).data("continueShoppingUrl");
			window.location = checkoutUrl;
		});
		
		$('.js-create-quote-button').click(function ()
		{
			$(this).prop("disabled", true);
			var createQuoteUrl = $(this).data("createQuoteUrl");
			window.location = createQuoteUrl;
		});

		
		$('.expressCheckoutButton').click(function()
				{
					document.getElementById("expressCheckoutCheckbox").checked = true;
		});
		
		$(document).on("input",".confirmGuestEmail,.guestEmail",function(){
			  
			  var orginalEmail = $(".guestEmail").val();
			  var confirmationEmail = $(".confirmGuestEmail").val();
			  
			  if(orginalEmail === confirmationEmail){
			    $(".guestCheckoutBtn").removeAttr("disabled");
			  }else{
			     $(".guestCheckoutBtn").attr("disabled","disabled");
			  }
		});
		
		$('.js-worldline-continue-checkout-button').click(function () {
			var checkoutUrl = $(this).data("checkoutUrl");
			cartEntriesError = ACC.pickupinstore.validatePickupinStoreCartEntires();
			var form = $('#replenishmentForm');
			if (!cartEntriesError) {
				if (form.length && $('input[name=replenishmentOrder]').prop('checked')) {
                  var localeDateFormat = $('#replenishmentSchedule').data('dateForDatePicker');
                  var startDateEntered = $("#replenishmentStartDate").val();
                  var endDateEntered = $("#replenishmentEndDate").val();
                  let validateStartDate = ACC.worldlineCart.validateDate(startDateEntered, localeDateFormat);
                  let validateEndDate = ACC.worldlineCart.validateDate(endDateEntered, localeDateFormat);
                  let validateDates = ACC.worldlineCart.validateDates(endDateEntered, localeDateFormat);

                  if ((validateStartDate && validateEndDate) && validateDates) {
                      $(".replenishmentOrderClass").val(true);

                      var formData = new FormData(form[0]);

                      var options = {
                        'replenishmentOrder' : formData.get('replenishmentOrder'),
                        'replenishmentStartDate': formData.get('replenishmentStartDate'),
                        'replenishmentEndDate': formData.get('replenishmentEndDate'),
                        'nDays': $('.scheduleformD').is(":visible") ? formData.get('nDays') : "",
                        'nWeeks': $('.scheduleformW').is(":visible") ? formData.get('nWeeks') : "",
                        'nMonths': $('.scheduleformM').is(":visible") ? formData.get('nMonths') : "",
                        'nthDayOfMonth': $('.scheduleformM').is(":visible") ? formData.get('nthDayOfMonth') : "",
                        'nDaysOfWeek': $('.scheduleformW').is(":visible") ? formData.getAll('nDaysOfWeek').join(",") : "",
                        'replenishmentRecurrence': formData.get('replenishmentRecurrence')
                      };

                      $.ajax({
                          url: form.attr('action'),
                          type: 'POST',
                          data: options,
                          success: function () {
                              //Continue to checkout
                              ACC.checkout.bindStartCheckout(checkoutUrl);
                          }
                      });

                  } else {
                       if (!validateStartDate) {
                           ACC.worldlineCart.toggleReplenishmentScheduleStartDateError(true);
                       }
                       if (!validateEndDate) {
                           ACC.worldlineCart.toggleReplenishmentScheduleEndDateError(true);
                       }
                       if (!validateDates) {
                            ACC.worldlineCart.toggleReplenishmentScheduleWrongDatesError(true);
                       }
                  }
				} else {
                //TODO update this on phase 3.1.
				    var options = {
				        'replenishmentOrder' : false,
				        'replenishmentStartDate': "",
                        'replenishmentEndDate': "",
                        'nDays': "",
                        'nWeeks': "",
                        'nMonths': "",
                        'nthDayOfMonth': "",
                        'nDaysOfWeek': "",
                        'replenishmentRecurrence': ""
				    };

                      $.ajax({
                          url: ACC.config.encodedContextPath + "/cart/savePlaceOrderData",
                          type: 'POST',
                          data: options,
                          success: function () {
                              //Continue to checkout
                              ACC.checkout.bindStartCheckout(checkoutUrl);
                          }
                      });
				}
			}
			return false;
		});

	},

    bindPlaceOrder: function ()
	{
	    ACC.checkout.toggleActionButtons('.place-order-form');
	},

	toggleActionButtons: function (selector) {

        var cssClass = $(document).find(selector);
        var checkoutBtn = cssClass.find('.checkoutSummaryButton');
        var checkBox = cssClass.find('input[name=termsCheck]');
        var checkBoxRememberPaymentDetails = cssClass.find('input[name=cardDetailsCheck]');
        var checkedOnInit = $(this).prop('checked');
        if (checkBoxRememberPaymentDetails.length) {
            checkoutBtn.prop('disabled', !(checkedOnInit && checkBoxRememberPaymentDetails.prop('checked')));
        } else {
            checkoutBtn.prop('disabled', !checkedOnInit);
        }

        checkBox.on('click', function () {
            var checked = $(this).prop('checked');
            var rememberPaymentDetails = checkBoxRememberPaymentDetails.length ? checkBoxRememberPaymentDetails.prop('checked') : true;
            if (checked && rememberPaymentDetails) {
                checkoutBtn.prop('disabled', false);
            } else {
                checkoutBtn.prop('disabled', true);
            }
        });

        checkBoxRememberPaymentDetails.on('click', function () {
            var rememberPaymentDetails = $(this).prop('checked');
            var checked = checkBox.prop('checked');

            if (checked && rememberPaymentDetails) {
                checkoutBtn.prop('disabled', false);
            } else {
                checkoutBtn.prop('disabled', true);
            }
        });

        checkoutBtn.on('click', function(){
          checkoutBtn.prop('disabled', true);
          $('#placeOrderForm1').submit();
        });
    },

	bindStartCheckout: function(checkoutUrl) {
		var expressCheckoutObject = $('.express-checkout-checkbox');
		if (expressCheckoutObject.is(":checked")) {
		   window.location = expressCheckoutObject.data("expressCheckoutUrl");
		} else {
		   var flow = $('#selectAltCheckoutFlow').val();
		   if ( flow == undefined || flow == '' || flow == 'select-checkout') {
		       // No alternate flow specified, fallback to default behaviour
		       window.location = checkoutUrl;
		   } else {
		       // Fix multistep-pci flow
		       if ('multistep-pci' == flow) {
		       	flow = 'multistep';
		       }
		       var pci = $(ACC.checkout.selectPciOption).val();

		       // Build up the redirect URL
		       var redirectUrl = checkoutUrl + '/select-flow?flow=' + flow + '&pci=' + pci;
		       window.location = redirectUrl;
		   }
		}
	}

};
