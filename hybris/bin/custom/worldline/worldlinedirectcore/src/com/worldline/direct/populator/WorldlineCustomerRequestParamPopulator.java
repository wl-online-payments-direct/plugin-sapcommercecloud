package com.worldline.direct.populator;

import com.onlinepayments.domain.*;
import de.hybris.platform.commerceservices.customer.CustomerEmailResolutionService;
import de.hybris.platform.commerceservices.enums.CustomerType;
import de.hybris.platform.commerceservices.strategies.CustomerNameStrategy;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;

import java.util.Locale;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.ACCOUNT_TYPE.CUSTOMER;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.ACCOUNT_TYPE.GUEST;

public class WorldlineCustomerRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private CommonI18NService commonI18NService;
    private CustomerEmailResolutionService customerEmailResolutionService;
    private CustomerNameStrategy customerNameStrategy;

    private static int PHONE_NUMBERS_LENGHT = 15;
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
        if (billingAddress.getRegion() != null) {
            address.setState(billingAddress.getRegion().getName());
        }
        address.setStreet(billingAddress.getLine1());
        address.setAdditionalInfo(billingAddress.getLine2());
        final Customer customer = new Customer();
        customer.setBillingAddress(address);
        customer.setLocale(getShopperLocale());
        if (abstractOrderModel.getUser() instanceof CustomerModel) {
            PersonalInformation personalInformation = new PersonalInformation();
            CustomerModel customerModel = (CustomerModel) abstractOrderModel.getUser();
            PersonalName personalName = new PersonalName();
            String[] firstAndLastName = customerNameStrategy.splitName(customerModel.getName());
            if (isGuestUser(customerModel) || firstAndLastName.length < 2) {
                personalName.setFirstName(billingAddress.getFirstname());
                personalName.setSurname(billingAddress.getLastname());
            } else {
                personalName.setFirstName(firstAndLastName[0]);
                personalName.setSurname(firstAndLastName[1]);
            }
            if (isGuestUser(customerModel) && billingAddress.getTitle() != null) {
                personalName.setTitle(billingAddress.getTitle().getName());

            } else if (customerModel.getTitle() != null) {
                personalName.setTitle(customerModel.getTitle().getName());
            }

            personalInformation.setName(personalName);
            customer.setPersonalInformation(personalInformation);
            ContactDetails contactDetails = new ContactDetails();
            contactDetails.setEmailAddress(customerEmailResolutionService.getEmailForCustomer(customerModel));
            contactDetails.setPhoneNumber(formatPhoneNumber(billingAddress.getPhone1()));
            contactDetails.setMobilePhoneNumber(formatPhoneNumber(billingAddress.getCellphone()));
            customer.setContactDetails(contactDetails);
            customer.setAccountType(isGuestUser(customerModel) ? GUEST : CUSTOMER);
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

    protected boolean isGuestUser(UserModel userModel) {
        return (userModel instanceof CustomerModel) && (CustomerType.GUEST.equals(((CustomerModel) userModel).getType()));
    }

    private String formatPhoneNumber(String phone) {
        phone = StringUtils.trim(StringUtils.defaultIfBlank(phone, StringUtils.EMPTY));
        if (phone.length() > PHONE_NUMBERS_LENGHT) {
            phone = phone.substring(0, PHONE_NUMBERS_LENGHT);
        }
        return phone;
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setCustomerEmailResolutionService(CustomerEmailResolutionService customerEmailResolutionService) {
        this.customerEmailResolutionService = customerEmailResolutionService;
    }

    @Required
    public void setCustomerNameStrategy(CustomerNameStrategy customerNameStrategy) {
        this.customerNameStrategy = customerNameStrategy;
    }
}
