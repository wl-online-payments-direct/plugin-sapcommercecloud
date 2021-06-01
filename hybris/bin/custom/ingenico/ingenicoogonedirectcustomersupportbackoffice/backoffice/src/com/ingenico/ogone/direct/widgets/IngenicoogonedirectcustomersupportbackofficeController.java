/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.ingenico.ogone.direct.widgets;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Label;

import com.hybris.cockpitng.util.DefaultWidgetController;

import com.ingenico.ogone.direct.services.IngenicoogonedirectcustomersupportbackofficeService;


public class IngenicoogonedirectcustomersupportbackofficeController extends DefaultWidgetController
{
	private static final long serialVersionUID = 1L;
	private Label label;

	@WireVariable
	private transient IngenicoogonedirectcustomersupportbackofficeService ingenicoogonedirectcustomersupportbackofficeService;

	@Override
	public void initialize(final Component comp)
	{
		super.initialize(comp);
		label.setValue(ingenicoogonedirectcustomersupportbackofficeService.getHello() + " IngenicoogonedirectcustomersupportbackofficeController");
	}
}
