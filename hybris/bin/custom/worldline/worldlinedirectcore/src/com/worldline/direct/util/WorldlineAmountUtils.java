package com.worldline.direct.util;

import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WorldlineAmountUtils {

    CommonI18NService commonI18NService;

    public long createAmount(BigDecimal amount, String currencyIso) {
        final CurrencyModel currency = commonI18NService.getCurrency(currencyIso);
        final Integer digits = currency.getDigits();

        return BigDecimal.TEN.pow(digits).multiply(amount.setScale(digits, RoundingMode.HALF_UP)).longValue();

    }

    public long createAmount(Double amount, String currencyIso) {
        return createAmount(BigDecimal.valueOf(amount), currencyIso);
    }

    public BigDecimal fromAmount(long amount, String currencyIso) {
        final CurrencyModel currency = commonI18NService.getCurrency(currencyIso);
        final Integer digits = currency.getDigits();
        return new BigDecimal(amount).movePointLeft(digits);
    }

    public BigDecimal fromAmount(double amount, String currencyIso) {
        final CurrencyModel currency = commonI18NService.getCurrency(currencyIso);
        final Integer digits = currency.getDigits();
        return new BigDecimal(amount).setScale(digits, RoundingMode.HALF_UP);
    }


    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }
}
