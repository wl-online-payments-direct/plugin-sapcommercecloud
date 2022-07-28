package com.worldline.direct.facade.impl;

import com.worldline.direct.facade.WorldlineReplenishmentFacade;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.converters.Converters;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineReplenishmentFacadeImpl implements WorldlineReplenishmentFacade {
    private UserService userService;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;
    private WorldlineCustomerAccountService worldlineCustomerAccountService;
    private ModelService modelService;
    private Converter<OrderModel, OrderHistoryData> orderHistoryConverter;

    @Override
    public SearchPageData<ScheduledCartData> getPagedReplenishmentHistory(PageableData pageableData) {
        final CustomerModel currentCustomer = (CustomerModel) userService.getCurrentUser();
        final SearchPageData<CartToOrderCronJobModel> jobResults = worldlineCustomerAccountService
                .getPagedCartToOrderCronJobsForUser(currentCustomer, pageableData);
        return convertPageData(jobResults, scheduledCartConverter);
    }

    protected <S, T> SearchPageData<T> convertPageData(final SearchPageData<S> source, final Converter<S, T> converter) {
        final SearchPageData<T> result = new SearchPageData<T>();
        result.setPagination(source.getPagination());
        result.setSorts(source.getSorts());
        result.setResults(Converters.convertAll(source.getResults(), converter));
        return result;
    }


    @Override
    public void cancelReplenishment(String jobCode, String user) {
        final CartToOrderCronJobModel cronJob = worldlineCustomerAccountService
                .getCartToOrderCronJob(jobCode);
        if (cronJob != null) {
            cronJob.setActive(Boolean.FALSE);
            modelService.save(cronJob);
        }

    }

    @Override
    public ScheduledCartData getReplenishmentOrderDetailsForCode(String code, String user) {
        ScheduledCartData scheduledCartData = null;
        final CartToOrderCronJobModel cronJob = worldlineCustomerAccountService
                .getCartToOrderCronJob(code);
        if (cronJob != null) {
            scheduledCartData = scheduledCartConverter.convert(cronJob);
        }

        return scheduledCartData;
    }

    @Override
    public SearchPageData<? extends OrderHistoryData> getPagedReplenishmentOrderHistory(String jobCode, PageableData pageableData) {
        final SearchPageData<OrderModel> ordersForJob = worldlineCustomerAccountService.getOrdersForJob(jobCode, pageableData);
        return convertPageData(ordersForJob, orderHistoryConverter);
    }

    @Required
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Required
    public void setScheduledCartConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter) {
        this.scheduledCartConverter = scheduledCartConverter;
    }

    @Required
    public void setWorldlineCustomerAccountService(WorldlineCustomerAccountService worldlineCustomerAccountService) {
        this.worldlineCustomerAccountService = worldlineCustomerAccountService;
    }

    @Required
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    @Required
    public void setOrderHistoryConverter(Converter<OrderModel, OrderHistoryData> orderHistoryConverter) {
        this.orderHistoryConverter = orderHistoryConverter;
    }
}
