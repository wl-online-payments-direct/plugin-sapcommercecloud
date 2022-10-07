/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.occ.mapping.mappers;

import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.b2bwebservicescommons.dto.order.DayOfWeekWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;
import ma.glasnost.orika.MappingContext;


public class WorldlineB2BPlaceOrderDataMapper extends AbstractCustomMapper<PlaceOrderData, ScheduleReplenishmentFormWsDTO>
{

	@Override
	public void mapAtoB(final PlaceOrderData a, final ScheduleReplenishmentFormWsDTO b, final MappingContext context)
	{
		// other fields are mapped automatically
		b.setNumberOfDays(a.getNDays());
		b.setNumberOfWeeks(a.getNWeeks());
		b.setNumberOfMonths(a.getNMonths());
		if (a.getNDaysOfWeek() != null)
		{
			b.setDaysOfWeek(mapperFacade.mapAsList(a.getNDaysOfWeek(), DayOfWeekWsDTO.class));
		}
		b.setRecurrencePeriod(a.getReplenishmentRecurrence().name());
	}

	@Override
	public void mapBtoA(final ScheduleReplenishmentFormWsDTO b, final PlaceOrderData a, final MappingContext context)
	{
		// other fields are mapped automatically
		a.setNDays(b.getNumberOfDays());
		a.setNWeeks(b.getNumberOfWeeks());
		a.setNMonths(b.getNumberOfMonths());
		if (b.getDaysOfWeek() != null)
		{
			a.setNDaysOfWeek(mapperFacade.mapAsList(b.getDaysOfWeek(), DayOfWeek.class));
		}
		if (b.getRecurrencePeriod() != null)
		{
			a.setReplenishmentRecurrence(Enum.valueOf(B2BReplenishmentRecurrenceEnum.class, b.getRecurrencePeriod()));
		}
	}

}
