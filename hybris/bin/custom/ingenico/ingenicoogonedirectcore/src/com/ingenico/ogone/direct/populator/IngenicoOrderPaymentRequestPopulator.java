package com.ingenico.ogone.direct.populator;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.math.BigDecimal;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.ingenico.direct.domain.Address;
import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.OrderReferences;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;

public class IngenicoOrderPaymentRequestPopulator implements Populator<CartModel, CreatePaymentRequest> {

    private IngenicoAmountUtils ingenicoAmountUtils;

    @Override
    public void populate(CartModel cartModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(cartModel, "cart cannot be null!");
        validateParameterNotNull(cartModel.getPaymentInfo(), "paymentInfo cannot be null!");
        validateParameterNotNull(cartModel.getPaymentInfo().getBillingAddress(), "billingAddress cannot be null!");

        final Order order = new Order();
        order.setAmountOfMoney(getAmoutOfMoney(cartModel));
        order.setCustomer(getCustomer(cartModel));
        order.setReferences(getReferences(cartModel));

        createPaymentRequest.setOrder(order);
    }

    private OrderReferences getReferences(CartModel cartModel) {
        final OrderReferences orderReferences = new OrderReferences();
        orderReferences.setMerchantReference(cartModel.getCode());
        return orderReferences;
    }

    private Customer getCustomer(CartModel cartModel) {
        final AddressModel billingAddress = cartModel.getPaymentInfo().getBillingAddress();

        Address address = new Address();
        address.setCity(billingAddress.getTown());
        address.setCountryCode(billingAddress.getCountry().getIsocode());
        address.setZip(billingAddress.getPostalcode());

        final Customer customer = new Customer();
        customer.setBillingAddress(address);

        return customer;
    }

    private AmountOfMoney getAmoutOfMoney(CartModel cartModel) {
        final AmountOfMoney amountOfMoney = new AmountOfMoney();
        final String currencyCode = cartModel.getCurrency().getIsocode();
        final long amount = ingenicoAmountUtils.createAmount(BigDecimal.valueOf(cartModel.getTotalPrice()), currencyCode);
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode(currencyCode);

        return amountOfMoney;
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }
}
