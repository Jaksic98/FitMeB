package com.consi.fitme.mapper;

import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.model.entity.TerminTemplate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TerminTemplatePatchMapper {

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  void applyPatch(UpdateTerminTemplateRequestDTO source, @MappingTarget TerminTemplate target);
}
