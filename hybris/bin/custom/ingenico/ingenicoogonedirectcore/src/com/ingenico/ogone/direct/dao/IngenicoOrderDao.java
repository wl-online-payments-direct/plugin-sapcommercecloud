package com.ingenico.ogone.direct.dao;

import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.daos.OrderDao;

public interface IngenicoOrderDao extends OrderDao {

    List<OrderModel> findIngenicoOrdersToCapture();

    OrderModel findIngenicoOrder(String orderCode);
}
