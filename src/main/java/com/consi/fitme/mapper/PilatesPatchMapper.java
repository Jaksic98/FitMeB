package com.consi.fitme.mapper;

import com.consi.fitme.dto.request.UpdatePilatesRequestDTO;
import com.consi.fitme.model.entity.Pilates;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PilatesPatchMapper {

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  void applyPatch(UpdatePilatesRequestDTO source, @MappingTarget Pilates target);
}
