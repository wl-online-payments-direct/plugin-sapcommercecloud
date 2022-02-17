package com.worldline.direct.dao;

import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.daos.OrderDao;

public interface WorldlineOrderDao extends OrderDao {

    List<OrderModel> findWorldlineOrdersToCapture();

    OrderModel findWorldlineOrder(String orderCode);
}
