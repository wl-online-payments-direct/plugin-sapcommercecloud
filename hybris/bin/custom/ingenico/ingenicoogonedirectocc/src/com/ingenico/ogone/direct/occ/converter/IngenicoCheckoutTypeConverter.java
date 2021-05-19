package com.ingenico.ogone.direct.occ.converter;


import de.hybris.platform.webservicescommons.mapping.WsDTOMapping;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.payment.dto.IngenicoCheckoutTypeWsDTO;

@WsDTOMapping
public class IngenicoCheckoutTypeConverter extends BidirectionalConverter<IngenicoCheckoutTypesEnum, IngenicoCheckoutTypeWsDTO> {

    @Override
    public IngenicoCheckoutTypeWsDTO convertTo(IngenicoCheckoutTypesEnum source, Type<IngenicoCheckoutTypeWsDTO> type, MappingContext mappingContext) {
        IngenicoCheckoutTypeWsDTO ingenicoCheckoutTypeWsDTO = new IngenicoCheckoutTypeWsDTO();
        if (source != null) {
            ingenicoCheckoutTypeWsDTO.setIngenicoCheckoutType(source.toString());
        }
        return ingenicoCheckoutTypeWsDTO;
    }

    @Override
    public IngenicoCheckoutTypesEnum convertFrom(IngenicoCheckoutTypeWsDTO source, Type<IngenicoCheckoutTypesEnum> type, MappingContext mappingContext) {
        return source != null && source.getIngenicoCheckoutType() != null ?
                IngenicoCheckoutTypesEnum.valueOf(source.getIngenicoCheckoutType()) : null;
    }
}
