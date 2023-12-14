package com.worldline.direct.populator.hostedcheckout;

import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreateMandateRequest;
import com.onlinepayments.domain.SepaDirectDebitPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.SepaDirectDebitPaymentProduct771SpecificInputBase;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import org.springframework.beans.factory.annotation.Required;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;

public class WorldlineSepaDirectDebitPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private static final String SMS = "SMS";
    private CommonI18NService commonI18NService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();
        if (WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(paymentInfo)) {
            SepaDirectDebitPaymentMethodSpecificInputBase sepaDirectDebit = new SepaDirectDebitPaymentMethodSpecificInputBase();
            SepaDirectDebitPaymentProduct771SpecificInputBase specificInputBase = new SepaDirectDebitPaymentProduct771SpecificInputBase();
            CreateMandateRequest createMandateRequest = new CreateMandateRequest();
            createMandateRequest.setLanguage(commonI18NService.getCurrentLanguage().getIsocode());
            createMandateRequest.setCustomerReference(abstractOrderModel.getCode() + "_" + System.currentTimeMillis());
            createMandateRequest.setRecurrenceType(WorldlinedirectcoreConstants.SEPA_RECURRING_TYPE.UNIQUE.getValue());
            createMandateRequest.setSignatureType(SMS);
            specificInputBase.setMandate(createMandateRequest);
            sepaDirectDebit.setPaymentProductId(PAYMENT_METHOD_SEPA);
            sepaDirectDebit.setPaymentProduct771SpecificInput(specificInputBase);
            createHostedCheckoutRequest.setSepaDirectDebitPaymentMethodSpecificInput(sepaDirectDebit);
        }
    }

    @Required
    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }
}
