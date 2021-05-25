package com.ingenico.ogone.direct.occ.helpers;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.payment.dto.DirectoryEntryWsDTO;
import com.ingenico.ogone.direct.payment.dto.HostedTokenizationResponseWsDTO;
import com.ingenico.ogone.direct.payment.dto.PaymentProductListWsDTO;
import com.ingenico.ogone.direct.payment.dto.ProductDirectoryWsDTO;
import com.ingenico.ogone.direct.payment.dto.SavedTokenWsDTO;
import com.ingenico.ogone.direct.payment.dto.SavedTokenListWsDTO;


@Component("ingenicoHelper")
public class IngenicoHelper {
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "ingenicoUserFacade")
    private IngenicoUserFacade ingenicoUserFacade;

    private int getIdealIndex(List<PaymentProduct> availablePaymentMethods) {
        return Iterables.indexOf(availablePaymentMethods, paymentProduct -> PAYMENT_METHOD_IDEAL == paymentProduct.getId());
    }

    public void fillIdealIssuers(PaymentProductListWsDTO paymentProductListWsDTO, List<PaymentProduct> availablePaymentMethods, String fields) {
        final List<DirectoryEntry> idealIssuers = ingenicoCheckoutFacade.getIdealIssuers(availablePaymentMethods);

        if (CollectionUtils.isNotEmpty(idealIssuers)) {

            final List<DirectoryEntryWsDTO> directoryEntryListWsDTO = getDataMapper().mapAsList(idealIssuers, DirectoryEntryWsDTO.class, fields);
            ProductDirectoryWsDTO productDirectoryWsDTO = new ProductDirectoryWsDTO();
            productDirectoryWsDTO.setEntries(directoryEntryListWsDTO);

            final int idealIndex = getIdealIndex(availablePaymentMethods);
            paymentProductListWsDTO.getPaymentProducts().get(idealIndex).setProductDirectory(productDirectoryWsDTO);
        }
    }

    public void fillSavedTokens(HostedTokenizationResponseWsDTO hostedTokenizationResponseWsDTO, String fields) {
        final List<IngenicoPaymentInfoData> ingenicoPaymentInfos = ingenicoUserFacade.getIngenicoPaymentInfos(true);
        final SavedTokenListWsDTO savedTokenListWsDTO = new SavedTokenListWsDTO();
        savedTokenListWsDTO.setTokens(getDataMapper().mapAsList(ingenicoPaymentInfos, SavedTokenWsDTO.class, fields));
        hostedTokenizationResponseWsDTO.setSavedTokens(savedTokenListWsDTO);
    }

    public String buildReturnURL(HttpServletRequest request, String key) {
        final String returnURL = Config.getParameter(key);
        final Map<String, String> uriVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return String.format(returnURL, uriVars.get("baseSiteId"), uriVars.get("userId"), uriVars.get("cartId"));
    }


    protected DataMapper getDataMapper() {
        return dataMapper;
    }

    protected void setDataMapper(final DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

}
