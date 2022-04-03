package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct809SpecificInput;
import com.ingenico.direct.domain.RedirectionData;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.site.BaseSiteService;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationRedirectPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    private BaseSiteService baseSiteService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue().equals(paymentInfo.getPaymentMethod())) {
            createPaymentRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(abstractOrderModel));
        }

    }

    private RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(AbstractOrderModel orderModel) {
        RedirectPaymentMethodSpecificInput input = new RedirectPaymentMethodSpecificInput();
        input.setPaymentProductId(PAYMENT_METHOD_IDEAL);
        input.setPaymentProduct809SpecificInput(new RedirectPaymentProduct809SpecificInput());
        input.getPaymentProduct809SpecificInput().setIssuerId(((WorldlinePaymentInfoModel) orderModel.getPaymentInfo()).getPaymentProductDirectoryId());
        input.setRedirectionData(new RedirectionData());
        input.getRedirectionData().setReturnUrl(siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(),
                true, "/checkout/multi/worldline/hosted-tokenization/handle3ds/" + orderModel.getCode()));
        return input;
    }


    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }
}
