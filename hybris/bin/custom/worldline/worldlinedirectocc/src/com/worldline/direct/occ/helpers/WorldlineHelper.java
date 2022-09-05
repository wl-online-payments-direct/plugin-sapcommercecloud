package com.worldline.direct.occ.helpers;

import com.google.common.collect.Iterables;
import com.onlinepayments.domain.DirectoryEntry;
import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectoccWebConstants;
import com.worldline.direct.enums.OrderType;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.payment.dto.DirectoryEntryWsDTO;
import com.worldline.direct.payment.dto.HostedTokenizationResponseWsDTO;
import com.worldline.direct.payment.dto.PaymentProductListWsDTO;
import com.worldline.direct.payment.dto.ProductDirectoryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL;


@Component("worldlineHelper")
public class WorldlineHelper {
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;

    @Resource(name = "worldlinePaymentProductFilterStrategyFactory")
    private WorldlinePaymentProductFilterStrategyFactory worldlinePaymentProductFilterStrategyFactory;

    private int getIdealIndex(List<PaymentProduct> availablePaymentMethods) {
        return Iterables.indexOf(availablePaymentMethods, paymentProduct -> PAYMENT_METHOD_IDEAL == paymentProduct.getId());
    }

    public void fillIdealIssuers(PaymentProductListWsDTO paymentProductListWsDTO, List<PaymentProduct> availablePaymentMethods, String fields) {
        final List<DirectoryEntry> idealIssuers = worldlineCheckoutFacade.getIdealIssuers(availablePaymentMethods);

        if (CollectionUtils.isNotEmpty(idealIssuers)) {

            final List<DirectoryEntryWsDTO> directoryEntryListWsDTO = getDataMapper().mapAsList(idealIssuers, DirectoryEntryWsDTO.class, fields);
            ProductDirectoryWsDTO productDirectoryWsDTO = new ProductDirectoryWsDTO();
            productDirectoryWsDTO.setEntries(directoryEntryListWsDTO);

            final int idealIndex = getIdealIndex(availablePaymentMethods);
            paymentProductListWsDTO.getPaymentProducts().get(idealIndex).setProductDirectory(productDirectoryWsDTO);
        }
    }

    public void fillSavedPaymentDetails(HostedTokenizationResponseWsDTO hostedTokenizationResponseWsDTO, String fields) {
        final List<PaymentProduct> availablePaymentMethods = worldlinePaymentProductFilterStrategyFactory.filter(worldlineCheckoutFacade.getAvailablePaymentMethods(), WorldlinePaymentProductFilterEnum.ACTIVE_PAYMENTS).get();
        final List<WorldlinePaymentInfoData> worldlinePaymentInfos = worldlineUserFacade.getWorldlinePaymentInfosForPaymentProducts(availablePaymentMethods, Boolean.TRUE);
        final PaymentDetailsListWsDTO paymentDetailsListWsDTO = new PaymentDetailsListWsDTO();
        paymentDetailsListWsDTO.setPayments(getDataMapper().mapAsList(worldlinePaymentInfos, PaymentDetailsWsDTO.class, fields));
        hostedTokenizationResponseWsDTO.setSavedPaymentDetails(paymentDetailsListWsDTO);
    }

    public String buildReturnURL(HttpServletRequest request, String key) {
        final String returnURL = Config.getParameter(key);
        final Map<String, String> uriVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return String.format(returnURL, uriVars.get("baseSiteId"), uriVars.get("userId"), "_orderCode_", request.getParameter("cartId"));
    }
    public String buildRecurringReturnURL(HttpServletRequest request, String key,Boolean isRecurring) {
        final String returnURL = Config.getParameter(key);
        final Map<String, String> uriVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return String.format(returnURL, uriVars.get("baseSiteId"),(isRecurring ? OrderType.SCHEDULE_REPLENISHMENT_ORDER : OrderType.PLACE_ORDER), uriVars.get("userId"), "_cartId_", request.getParameter("cartId"));
    }


    protected DataMapper getDataMapper() {
        return dataMapper;
    }

    protected void setDataMapper(final DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

}
