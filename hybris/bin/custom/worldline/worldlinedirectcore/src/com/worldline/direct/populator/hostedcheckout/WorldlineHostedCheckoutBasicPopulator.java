package com.worldline.direct.populator.hostedcheckout;

import com.onlinepayments.domain.CardPaymentMethodSpecificInputForHostedCheckout;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.Order;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.facade.WorldlineUserFacade;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutBasicPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    public static final String HOSTED_CHECKOUT_RETURN_URL = "hostedCheckoutReturnUrl";

    private SessionService sessionService;
    private I18NService i18NService;

    private WorldlineUserFacade worldlineUserFacade;
    private Converter<AbstractOrderModel, Order> worldlineOrderParamConverter;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        createHostedCheckoutRequest.setHostedCheckoutSpecificInput(getHostedCheckoutSpecificInput(abstractOrderModel));
        createHostedCheckoutRequest.setOrder(worldlineOrderParamConverter.convert(abstractOrderModel));
    }

    private HostedCheckoutSpecificInput getHostedCheckoutSpecificInput(AbstractOrderModel abstractOrderModel) {
        HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
        hostedCheckoutSpecificInput.setIsRecurring(Boolean.FALSE);
        hostedCheckoutSpecificInput.setShowResultPage(Boolean.FALSE);
        hostedCheckoutSpecificInput.setLocale(i18NService.getCurrentLocale().toString());
        hostedCheckoutSpecificInput.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInputForHostedCheckout(abstractOrderModel));
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();
        hostedCheckoutSpecificInput.setTokens(getSavedTokens(paymentInfo.getId()));

        hostedCheckoutSpecificInput.setReturnUrl(getReturnUrlFromSession());

        return hostedCheckoutSpecificInput;
    }

    private CardPaymentMethodSpecificInputForHostedCheckout getCardPaymentMethodSpecificInputForHostedCheckout(AbstractOrderModel abstractOrderModel)
    {
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();
        CardPaymentMethodSpecificInputForHostedCheckout cardPaymentMethodSpecificInputForHostedCheckout=new CardPaymentMethodSpecificInputForHostedCheckout();
        cardPaymentMethodSpecificInputForHostedCheckout.setGroupCards(WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS==paymentInfo.getId());
        return cardPaymentMethodSpecificInputForHostedCheckout;
    }

    private String getSavedTokens(Integer paymentMethodId) {
        final List<String> savedTokens = worldlineUserFacade.getSavedTokensForPaymentMethod(paymentMethodId);
        if (CollectionUtils.isNotEmpty(savedTokens)) {
            return String.join(",", savedTokens);
        }
        return StringUtils.EMPTY;
    }

    private String getReturnUrlFromSession() {
        return sessionService.getAttribute(HOSTED_CHECKOUT_RETURN_URL);
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
    }

    public void setWorldlineUserFacade(WorldlineUserFacade worldlineUserFacade) {
        this.worldlineUserFacade = worldlineUserFacade;
    }

    public void setWorldlineOrderParamConverter(Converter<AbstractOrderModel, Order> worldlineOrderParamConverter) {
        this.worldlineOrderParamConverter = worldlineOrderParamConverter;
    }
}
