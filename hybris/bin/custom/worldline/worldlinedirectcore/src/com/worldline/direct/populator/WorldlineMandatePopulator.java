package com.worldline.direct.populator;

import com.worldline.direct.address.data.WorldlineMandateAddress;
import com.worldline.direct.contact.data.WorldlinePersonalInformation;
import com.worldline.direct.customer.data.WorldlineMandateCustomer;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.order.data.WorldlineMandateDetail;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineMandatePopulator implements Populator<WorldlineMandateModel, WorldlineMandateDetail> {
    private EnumerationService enumerationService;

    @Override
    public void populate(WorldlineMandateModel source, WorldlineMandateDetail target) throws ConversionException {
        target.setAlias(source.getAlias());
        target.setCustomerReference(source.getCustomerReference());
        target.setStatus(enumerationService.getEnumerationName(source.getStatus()));
        target.setUniqueMandateReference(source.getUniqueMandateReference());
        target.setRecurrenceType(enumerationService.getEnumerationName(source.getRecurrenceType()));
        target.setCustomer(createMandateCustomer(source));

    }

    private WorldlineMandateCustomer createMandateCustomer(WorldlineMandateModel worldlineMandateModel) {
        WorldlineMandateCustomer mandateCustomer = new WorldlineMandateCustomer();
        mandateCustomer.setCompanyName(worldlineMandateModel.getCompanyName());
        mandateCustomer.setEmailAddress(worldlineMandateModel.getEmailAddress());

        mandateCustomer.setIban(worldlineMandateModel.getIban());
        mandateCustomer.setPersonalInformation(createPersonalInformation(worldlineMandateModel));
        mandateCustomer.setAddress(createMandateAddress(worldlineMandateModel));
        return mandateCustomer;
    }

    private WorldlineMandateAddress createMandateAddress(WorldlineMandateModel worldlineMandateModel) {
        WorldlineMandateAddress mandateAddress = new WorldlineMandateAddress();
        mandateAddress.setCity(worldlineMandateModel.getCity());
        if (worldlineMandateModel.getCountry() != null) {
            mandateAddress.setCountry(worldlineMandateModel.getCountry().getName());
        }
        mandateAddress.setHouseNumber(worldlineMandateModel.getHouseNumber());
        mandateAddress.setZip(worldlineMandateModel.getZip());
        mandateAddress.setStreet(worldlineMandateModel.getStreet());
        return mandateAddress;
    }

    private WorldlinePersonalInformation createPersonalInformation(WorldlineMandateModel worldlineMandateModel) {
        WorldlinePersonalInformation personalInformation = new WorldlinePersonalInformation();
        personalInformation.setFirstName(worldlineMandateModel.getFirstName());
        personalInformation.setLastName(worldlineMandateModel.getLastName());
        if (worldlineMandateModel.getTitle() != null) {
            personalInformation.setTitle(worldlineMandateModel.getTitle().getName());
        }
        return personalInformation;
    }

    @Required
    public void setEnumerationService(EnumerationService enumerationService) {
        this.enumerationService = enumerationService;
    }
}
