package com.ingenico.ogone.direct.populator;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.math.BigDecimal;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.ingenico.direct.domain.Address;
import com.ingenico.direct.domain.AddressPersonal;
import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.OrderReferences;
import com.ingenico.direct.domain.PersonalName;
import com.ingenico.direct.domain.Shipping;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;

public class IngenicoOrderRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private IngenicoAmountUtils ingenicoAmountUtils;

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
        if(deliveryAddress.getRegion()!=null){
            address.setState(deliveryAddress.getRegion().getName());
        }
        address.setStreet(deliveryAddress.getLine1());
        address.setAdditionalInfo(deliveryAddress.getLine2());

        final PersonalName personalName = new PersonalName();
        personalName.setFirstName(deliveryAddress.getFirstname());
        personalName.setSurname(deliveryAddress.getLastname());
        if(deliveryAddress.getTitle()!=null) {
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
        final long amount = ingenicoAmountUtils.createAmount(abstractOrderModel.getTotalPrice(), currencyCode);
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode(currencyCode);

        return amountOfMoney;
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }
}
