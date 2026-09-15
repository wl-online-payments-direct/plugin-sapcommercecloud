package com.worldline.gopay.populator;

import com.onlinepayments.domain.*;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.util.WorldlineAmountUtils;
import com.worldline.gopay.util.WorldlinePaymentProductUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.util.TaxValue;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;

import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.ADDRESS_INDICATEUR.NEW;
import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.ADDRESS_INDICATEUR.SAME_AS_BILLING;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineOrderRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");

        order.setAmountOfMoney(getAmoutOfMoney(abstractOrderModel));
        order.setShipping(getShipping(abstractOrderModel));
        order.setReferences(getReferences(abstractOrderModel));
        order.setDiscount(getOrderLevelDiscount(abstractOrderModel));
        if (BooleanUtils.isTrue(abstractOrderModel.getStore().getWorldlineConfiguration().isApplySurcharge())) {
            order.withSurchargeSpecificInput(new SurchargeSpecificInput()).getSurchargeSpecificInput().setMode("on-behalf-of");
        }
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
        if (abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel && WorldlinePaymentProductUtils.isPaymentByKlarna(((WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo()))) {
            shipping.setShippingCost(worldlineAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), abstractOrderModel.getCurrency().getIsocode()));
        }
        shipping.setAddressIndicator(BooleanUtils.isTrue(deliveryAddress.getShippingAddress()) ? SAME_AS_BILLING : NEW);
        long shippingCostTax = getShippingTaxForOrder(abstractOrderModel);
        shipping.setShippingCost(getShippingCostsForOrder(abstractOrderModel, shippingCostTax));
        shipping.setShippingCostTax(shippingCostTax);
        return shipping;
    }

    private OrderReferences getReferences(AbstractOrderModel abstractOrderModel) {
        final OrderReferences orderReferences = new OrderReferences();
        orderReferences.setMerchantReference(abstractOrderModel.getCode());

        WorldlineConfigurationModel worldlineConfiguration = abstractOrderModel.getStore().getWorldlineConfiguration();
        if (worldlineConfiguration != null) {
            String merchantDescriptor;
            if (!StringUtils.isBlank(worldlineConfiguration.getMerchantName())) {
                merchantDescriptor = worldlineConfiguration.getMerchantName();
            } else {
                merchantDescriptor = worldlineConfiguration.getMerchantID();
            }
            if (merchantDescriptor != null && merchantDescriptor.length() > 15) {
                merchantDescriptor = merchantDescriptor.substring(0, 14);
            }
            orderReferences.setDescriptor(merchantDescriptor);
        }
        return orderReferences;
    }

    private AmountOfMoney getAmoutOfMoney(AbstractOrderModel abstractOrderModel) {
        final AmountOfMoney amountOfMoney = new AmountOfMoney();
        final String currencyCode = abstractOrderModel.getCurrency().getIsocode();
        final long amount;
        BigDecimal totalAmountToSend = getTotalAmountToSend(abstractOrderModel);
        if (abstractOrderModel.getPaymentCost() > 0.0d) {// subtract the surcharge so the amount that is sent to WL is the one expected /HTP/ first transaction
            totalAmountToSend = totalAmountToSend.subtract(BigDecimal.valueOf(abstractOrderModel.getPaymentCost()));
        }

        amount = worldlineAmountUtils.createAmount(totalAmountToSend, abstractOrderModel.getCurrency().getIsocode());
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode(currencyCode);

        return amountOfMoney;
    }

    private long getShippingCostsForOrder(AbstractOrderModel order) {
        return worldlineAmountUtils.createAmount(order.getDeliveryCost(), order.getCurrency().getIsocode());
    }

    private long getShippingCostsForOrder(AbstractOrderModel order, long shippingCostTax) {
        if (Boolean.TRUE.equals(order.getNet())) {
            return getShippingCostsForOrder(order);
        }
        long shippingCost = getShippingCostsForOrder(order) - shippingCostTax;
        return Math.max(0L, shippingCost);
    }

    private BigDecimal getTotalAmountToSend(AbstractOrderModel order) {
        BigDecimal totalAmount = BigDecimal.valueOf(order.getTotalPrice());
        if (Boolean.TRUE.equals(order.getNet())) {
            totalAmount = totalAmount.add(getTotalTax(order));
        }
        return totalAmount;
    }

    private long getShippingTaxForOrder(AbstractOrderModel order) {
        BigDecimal shippingTax = getTotalTax(order).subtract(getEntryTaxTotal(order));
        if (shippingTax.compareTo(BigDecimal.ZERO) < 0) {
            shippingTax = BigDecimal.ZERO;
        }
        return worldlineAmountUtils.createAmount(shippingTax, order.getCurrency().getIsocode());
    }

    private BigDecimal getTotalTax(AbstractOrderModel order) {
        Double totalTax = order.getTotalTax();
        return totalTax == null ? BigDecimal.ZERO : BigDecimal.valueOf(totalTax);
    }

    private BigDecimal getEntryTaxTotal(AbstractOrderModel order) {
        BigDecimal entryTaxTotal = BigDecimal.ZERO;
        Collection<AbstractOrderEntryModel> entries = order.getEntries();
        if (entries == null) {
            return entryTaxTotal;
        }
        for (AbstractOrderEntryModel entry : entries) {
            entryTaxTotal = entryTaxTotal.add(getEntryTaxTotal(entry));
        }
        return entryTaxTotal;
    }

    private BigDecimal getEntryTaxTotal(AbstractOrderEntryModel entry) {
        BigDecimal entryTaxTotal = BigDecimal.ZERO;
        Collection<TaxValue> taxValues = entry.getTaxValues();
        if (taxValues == null) {
            return entryTaxTotal;
        }
        for (TaxValue taxValue : taxValues) {
            entryTaxTotal = entryTaxTotal.add(BigDecimal.valueOf(taxValue.getAppliedValue()));
        }
        return entryTaxTotal;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

    private Discount getOrderLevelDiscount(AbstractOrderModel order) {
        Discount discount = new Discount();
        CurrencyModel currency = order.getCurrency();

        if (currency == null) {
            throw new UnsupportedOperationException("Cannot calculate discount on Order with no Currency assigned.");
        }
        discount.setAmount(worldlineAmountUtils.createAmount(order.getTotalDiscounts(), currency.getIsocode()));
        return discount;
    }
}
