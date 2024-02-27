package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.SepaDirectDebitPaymentMethodSpecificInput;
import com.onlinepayments.domain.SepaDirectDebitPaymentProduct771SpecificInput;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.commons.lang.StringUtils;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationSepaPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();
        if (WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(paymentInfo) && paymentInfo.getMandateDetail() != null) {
            SepaDirectDebitPaymentMethodSpecificInput sepaDirectDebit = new SepaDirectDebitPaymentMethodSpecificInput();
            SepaDirectDebitPaymentProduct771SpecificInput specificInputBase = new SepaDirectDebitPaymentProduct771SpecificInput();
            specificInputBase.withExistingUniqueMandateReference(paymentInfo.getMandateDetail().getUniqueMandateReference());
            sepaDirectDebit.setPaymentProductId(PAYMENT_METHOD_SEPA);
            sepaDirectDebit.setPaymentProduct771SpecificInput(specificInputBase);
            createPaymentRequest.setSepaDirectDebitPaymentMethodSpecificInput(sepaDirectDebit);
        }

    }
}
