package com.worldline.direct.populator;

import com.ingenico.direct.domain.*;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineOrderRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");

        order.setAmountOfMoney(getAmoutOfMoney(abstractOrderModel));
        order.setShipping(getShipping(abstractOrderModel));
        order.setReferences(getReferences(abstractOrderModel));

    }

    private Shipping getShipping(AbstractOrderModel abstractOrderModel) {
        if (abstractOrderModel.getDeliveryAddress() == null) {
            return null;
        }
        Shipping shipping = new Shipping();
        final AddressModel deliveryAddress = abstractOrderModel.getDeliveryAddress();

        AddressPersonal address = new AddressPersonal();
        address.setZip(deliveryAddress.getPostalcode());
        address.setCountryCode(deliveryAddress.getCountry().getIsocode());
        address.setCity(deliveryAddress.getTown());
        if (deliveryAddress.getRegion() != null) {
            address.setState(deliveryAddress.getRegion().getName());
        }
        address.setStreet(deliveryAddress.getLine1());
        address.setAdditionalInfo(deliveryAddress.getLine2());

        final PersonalName personalName = new PersonalName();
        personalName.setFirstName(deliveryAddress.getFirstname());
        personalName.setSurname(deliveryAddress.getLastname());
        if (deliveryAddress.getTitle() != null) {
            personalName.setTitle(deliveryAddress.getTitle().getName());
        }
        address.setName(personalName);

        shipping.setAddress(address);
        shipping.setEmailAddress(deliveryAddress.getEmail());

        return shipping;
    }

    private OrderReferences getReferences(AbstractOrderModel abstractOrderModel) {
        final OrderReferences orderReferences = new OrderReferences();
        orderReferences.setMerchantReference(abstractOrderModel.getCode());
        return orderReferences;
    }

    private AmountOfMoney getAmoutOfMoney(AbstractOrderModel abstractOrderModel) {
        final AmountOfMoney amountOfMoney = new AmountOfMoney();
        final String currencyCode = abstractOrderModel.getCurrency().getIsocode();
        final long amount;
        amount = worldlineAmountUtils.getOrderPrice(abstractOrderModel);
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode(currencyCode);

        return amountOfMoney;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }
}
