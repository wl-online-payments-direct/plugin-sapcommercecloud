/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.constants;

/**
 * Global class for all worldlinedirectocc web constants. You can add global constants for your extension into this class.
 */
public interface WorldlinedirectoccWebConstants
{
	interface  URL
	{
		interface Checkout
		{
			interface Payment
			{
				interface HOP {
					interface Option {
						String order = "PLACE_ORDER";
						String replenishment = "SCHEDULE_REPLENISHMENT_ORDER";
					}

				}

			}
		}

	}
}
