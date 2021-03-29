package com.ingenico.ogone.direct.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

public class IngenicoAmountUtils {

    CommonI18NService commonI18NService;

    public long createAmount(BigDecimal amount, String currencyIso) {
        final CurrencyModel currency = commonI18NService.getCurrency(currencyIso);
        final Integer digits = currency.getDigits();

        return BigDecimal.TEN.pow(digits).multiply(amount.setScale(digits, RoundingMode.HALF_UP)).longValue();

    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }


}