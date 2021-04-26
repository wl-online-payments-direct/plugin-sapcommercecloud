package com.ingenico.ogone.direct.occ.helpers;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;

import javax.annotation.Resource;
import java.util.List;

import de.hybris.platform.webservicescommons.mapping.DataMapper;

import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.payment.dto.DirectoryEntryListWsDTO;
import com.ingenico.ogone.direct.payment.dto.PaymentProductListWsDTO;


@Component("ingenicoHelper")
public class IngenicoHelper {
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    private int getIdealIndex(List<PaymentProduct> availablePaymentMethods) {
      return Iterables.indexOf(availablePaymentMethods, paymentProduct -> PAYMENT_METHOD_IDEAL.equals(paymentProduct.getId()));
    }

    public void fillIdealIssuers(PaymentProductListWsDTO paymentProductListWsDTO, List<PaymentProduct> availablePaymentMethods,String fields) {
        final List<DirectoryEntry> idealIssuers = ingenicoCheckoutFacade.getIdealIssuers(availablePaymentMethods);

        if(CollectionUtils.isNotEmpty(idealIssuers)) {
            final DirectoryEntryListWsDTO directoryEntryListWsDTO = getDataMapper().map(idealIssuers, DirectoryEntryListWsDTO.class, fields);
            final int idealIndex = getIdealIndex(availablePaymentMethods);
            paymentProductListWsDTO.getPaymentProducts().get(idealIndex).setDirectoryEntries(directoryEntryListWsDTO);
        }
    }

    protected DataMapper getDataMapper() {
        return dataMapper;
    }

    protected void setDataMapper(final DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

}
