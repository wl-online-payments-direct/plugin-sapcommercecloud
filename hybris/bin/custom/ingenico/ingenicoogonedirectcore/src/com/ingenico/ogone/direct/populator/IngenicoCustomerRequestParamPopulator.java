package com.ingenico.ogone.direct.populator;

import java.util.Locale;

import de.hybris.platform.commerceservices.customer.CustomerEmailResolutionService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

import com.ingenico.direct.domain.Address;
import com.ingenico.direct.domain.ContactDetails;
import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.Order;

public class IngenicoCustomerRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private CommonI18NService commonI18NService;
    private CustomerEmailResolutionService customerEmailResolutionService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        order.setCustomer(getCustomer(abstractOrderModel));
    }

    private Customer getCustomer(AbstractOrderModel abstractOrderModel) {
        final AddressModel billingAddress = abstractOrderModel.getPaymentInfo().getBillingAddress();

        Address address = new Address();
        address.setCity(billingAddress.getTown());
        address.setCountryCode(billingAddress.getCountry().getIsocode());
        address.setZip(billingAddress.getPostalcode());

        final Customer customer = new Customer();
        customer.setBillingAddress(address);
        customer.setLocale(getShopperLocale());

        if (abstractOrderModel.getUser() instanceof CustomerModel) {
            ContactDetails contactDetails = new ContactDetails();
            contactDetails.setEmailAddress(customerEmailResolutionService.getEmailForCustomer((CustomerModel) abstractOrderModel.getUser()));
            customer.setContactDetails(contactDetails);
        }
        return customer;
    }

    protected String getShopperLocale() {
        final LanguageModel currentLanguage = commonI18NService.getCurrentLanguage();
        if (currentLanguage != null) {
            return commonI18NService.getLocaleForLanguage(currentLanguage).toString();
        }
        return Locale.ENGLISH.toString();
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setCustomerEmailResolutionService(CustomerEmailResolutionService customerEmailResolutionService) {
        this.customerEmailResolutionService = customerEmailResolutionService;
    }
}
