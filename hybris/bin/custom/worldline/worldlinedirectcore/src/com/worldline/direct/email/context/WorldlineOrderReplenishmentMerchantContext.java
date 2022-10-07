package com.worldline.direct.email.context;

import de.hybris.platform.acceleratorservices.model.cms2.pages.EmailPageModel;
import de.hybris.platform.acceleratorservices.process.email.context.AbstractEmailContext;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

public class WorldlineOrderReplenishmentMerchantContext extends AbstractEmailContext<ReplenishmentProcessModel> {
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;
    private ScheduledCartData scheduledCartData;
    private CommonI18NService commonI18NService;

    @Override
    public void init(final ReplenishmentProcessModel replenishmentProcessModel, final EmailPageModel emailPageModel) {
        super.init(replenishmentProcessModel, emailPageModel);

        put(EMAIL, replenishmentProcessModel.getCartToOrderCronJob().getCart().getStore().getMerchant(commonI18NService.getLocaleForLanguage(getEmailLanguage(replenishmentProcessModel))));
        scheduledCartData = scheduledCartConverter.convert(replenishmentProcessModel.getCartToOrderCronJob());
    }

    @Override
    protected BaseSiteModel getSite(ReplenishmentProcessModel businessProcessModel) {
        return businessProcessModel.getSite();
    }

    @Override
    protected CustomerModel getCustomer(ReplenishmentProcessModel businessProcessModel) {
        return businessProcessModel.getCustomer();
    }

    @Override
    protected LanguageModel getEmailLanguage(ReplenishmentProcessModel businessProcessModel) {
        return businessProcessModel.getLanguage();
    }

    public void setScheduledCartConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter) {
        this.scheduledCartConverter = scheduledCartConverter;
    }

    public ScheduledCartData getScheduledCartData() {
        return scheduledCartData;
    }

    public void setScheduledCartData(ScheduledCartData scheduledCartData) {
        this.scheduledCartData = scheduledCartData;
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

}
