/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.occ.controllers.v2.validator;

import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.commercefacades.order.data.CartData;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


/**
 * B2B cart validator. Checks if cart is calculated and if needed values are filled.
 */
@Component("worldlineB2BPlaceOrderCartValidator")
public class WorldlineB2BPlaceOrderCartValidator implements Validator
{
	@Override
	public boolean supports(final Class<?> clazz)
	{
		return CartData.class.equals(clazz);
	}

	@Override
	public void validate(final Object target, final Errors errors)
	{
		final CartData cart = (CartData) target;

		if (!cart.isCalculated())
		{
			errors.reject("cart.notCalculated");
		}

		if (cart.getDeliveryAddress() == null)
		{
			errors.reject("cart.deliveryAddressNotSet");
		}

		if (cart.getDeliveryMode() == null)
		{
			errors.reject("cart.deliveryModeNotSet");
		}

		if (CheckoutPaymentType.CARD.getCode().equals(cart.getPaymentType().getCode()))
		{
			if (cart.getWorldlinePaymentInfo() == null)
			{
				errors.reject("cart.paymentInfoNotSet");
			}
		}
		else
		{
			if (cart.getCostCenter() == null)
			{
				errors.reject("cart.costCenterNotSet");
			}
		}
	}
}
