package com.worldline.direct.email.context;

import de.hybris.platform.acceleratorservices.model.cms2.pages.EmailPageModel;
import de.hybris.platform.acceleratorservices.process.email.context.AbstractEmailContext;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.processengine.helpers.ProcessParameterHelper;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import java.util.List;

import static com.worldline.direct.actions.replenishment.WorldlineValidateCartAction.CART_MODIFICATIONS_PARAM;

public class WorldlineOrderReplenishmentCartNonValidContext extends AbstractEmailContext<ReplenishmentProcessModel> {
    private ProcessParameterHelper processParameterHelper;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;

    private List<CartModificationData> cartModifications;
    private ScheduledCartData scheduledCartData;

    @Override
    public void init(ReplenishmentProcessModel businessProcessModel, EmailPageModel emailPageModel) {
        super.init(businessProcessModel, emailPageModel);
        cartModifications= (List<CartModificationData>) processParameterHelper.getProcessParameterByName(businessProcessModel, CART_MODIFICATIONS_PARAM).getValue();
        scheduledCartData = scheduledCartConverter.convert(businessProcessModel.getCartToOrderCronJob());

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

    public void setProcessParameterHelper(ProcessParameterHelper processParameterHelper) {
        this.processParameterHelper = processParameterHelper;
    }

    public List<CartModificationData> getCartModifications() {

        return cartModifications;
    }

    public ScheduledCartData getScheduledCartData() {
        return scheduledCartData;
    }

    public void setScheduledCartConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter) {
        this.scheduledCartConverter = scheduledCartConverter;
    }
}
