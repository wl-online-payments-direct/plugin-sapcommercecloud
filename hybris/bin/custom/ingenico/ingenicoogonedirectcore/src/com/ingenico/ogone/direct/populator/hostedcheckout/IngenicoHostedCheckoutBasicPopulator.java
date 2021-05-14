package com.ingenico.ogone.direct.populator.hostedcheckout;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.List;

import com.ingenico.ogone.direct.constants.GeneratedIngenicoogonedirectcoreConstants;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.session.SessionService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.HostedCheckoutSpecificInput;
import com.ingenico.direct.domain.Order;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;

public class IngenicoHostedCheckoutBasicPopulator implements Populator<CartModel, CreateHostedCheckoutRequest> {

    private SessionService sessionService;
    private I18NService i18NService;

    private IngenicoUserFacade ingenicoUserFacade;
    private Converter<CartModel, Order> ingenicoOrderParamConverter;

    @Override
    public void populate(CartModel cartModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(cartModel, "cart cannot be null!");
        createHostedCheckoutRequest.setHostedCheckoutSpecificInput(getHostedCheckoutSpecificInput(cartModel));
        createHostedCheckoutRequest.setOrder(ingenicoOrderParamConverter.convert(cartModel));
    }

    private HostedCheckoutSpecificInput getHostedCheckoutSpecificInput(CartModel cartModel) {
        HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
        hostedCheckoutSpecificInput.setIsRecurring(Boolean.FALSE);
        hostedCheckoutSpecificInput.setShowResultPage(Boolean.FALSE);
        hostedCheckoutSpecificInput.setLocale(i18NService.getCurrentLocale().toString());

        final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cartModel.getPaymentInfo();
        hostedCheckoutSpecificInput.setTokens(getSavedTokens(paymentInfo.getId()));

        hostedCheckoutSpecificInput.setReturnUrl(getReturnUrlFromSession());

        return hostedCheckoutSpecificInput;
    }

    private String getSavedTokens(Integer paymentMethodId) {
        final List<String> savedTokens = ingenicoUserFacade.getSavedTokensForPaymentMethod(paymentMethodId);
        if (CollectionUtils.isNotEmpty(savedTokens)) {
            return String.join(",", savedTokens);
        }
        return StringUtils.EMPTY;
    }

    private String getReturnUrlFromSession() {
        return sessionService.getAttribute("hostedCheckoutReturnUrl");
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
    }

    public void setIngenicoUserFacade(IngenicoUserFacade ingenicoUserFacade) {
        this.ingenicoUserFacade = ingenicoUserFacade;
    }

    public void setIngenicoOrderParamConverter(Converter<CartModel, Order> ingenicoOrderParamConverter) {
        this.ingenicoOrderParamConverter = ingenicoOrderParamConverter;
    }
}
