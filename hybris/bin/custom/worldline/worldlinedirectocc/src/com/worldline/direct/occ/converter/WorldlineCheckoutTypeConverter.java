package com.worldline.direct.occ.converter;


import de.hybris.platform.webservicescommons.mapping.WsDTOMapping;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.payment.dto.WorldlineCheckoutTypeWsDTO;

@WsDTOMapping
public class WorldlineCheckoutTypeConverter extends BidirectionalConverter<WorldlineCheckoutTypesEnum, WorldlineCheckoutTypeWsDTO> {

    @Override
    public WorldlineCheckoutTypeWsDTO convertTo(WorldlineCheckoutTypesEnum source, Type<WorldlineCheckoutTypeWsDTO> type, MappingContext mappingContext) {
        WorldlineCheckoutTypeWsDTO worldlineCheckoutTypeWsDTO = new WorldlineCheckoutTypeWsDTO();
        if (source != null) {
            worldlineCheckoutTypeWsDTO.setWorldlineCheckoutType(source.toString());
        }
        return worldlineCheckoutTypeWsDTO;
    }

    @Override
    public WorldlineCheckoutTypesEnum convertFrom(WorldlineCheckoutTypeWsDTO source, Type<WorldlineCheckoutTypesEnum> type, MappingContext mappingContext) {
        return source != null && source.getWorldlineCheckoutType() != null ?
                WorldlineCheckoutTypesEnum.valueOf(source.getWorldlineCheckoutType()) : null;
    }
}
