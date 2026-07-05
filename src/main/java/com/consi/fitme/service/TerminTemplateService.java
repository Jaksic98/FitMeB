package com.consi.fitme.service;

import com.consi.fitme.dto.TerminTemplateDTO;
import com.consi.fitme.dto.request.CreateTerminTemplateRequestDTO;
import com.consi.fitme.dto.request.UpdateTerminTemplateRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.termintemplate.InvalidTerminTemplateTimeRangeException;
import com.consi.fitme.exception.termintemplate.TerminTemplateNotFoundException;
import com.consi.fitme.exception.termintemplate.TerminTemplateOverlapException;
import com.consi.fitme.mapper.TerminTemplatePatchMapper;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.TerminTemplate;
import com.consi.fitme.repository.TerminTemplateRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TerminTemplateService {

  private final TerminTemplateRepository repository;
  private final TerminTemplatePatchMapper patchMapper;

  public List<TerminTemplateDTO> getAllTerminTemplates() {
    return repository.findAllByStatusNot(Status.DELETED).stream().map(this::toDto).toList();
  }

  public TerminTemplateDTO getTerminTemplate(Long id) {
    return toDto(findActiveOrInactiveById(id));
  }

  @Transactional
  public TerminTemplateDTO createTerminTemplate(
      CreateTerminTemplateRequestDTO createTerminTemplateRequestDTO) {
    DayOfWeek dayOfWeek = createTerminTemplateRequestDTO.getDayOfWeek();
    LocalTime startTime = createTerminTemplateRequestDTO.getStartTime();
    LocalTime endTime = createTerminTemplateRequestDTO.getEndTime();

    ensureValidTimeRange(startTime, endTime);
    ensureNoOverlap(dayOfWeek, startTime, endTime, null);

    TerminTemplate terminTemplate =
        TerminTemplate.builder().dayOfWeek(dayOfWeek).startTime(startTime).endTime(endTime).build();
    TerminTemplate savedTerminTemplate = repository.save(terminTemplate);

    return toDto(savedTerminTemplate);
  }

  @Transactional
  public TerminTemplateDTO updateTerminTemplate(
      Long id, UpdateTerminTemplateRequestDTO updateTerminTemplateRequestDTO) {
    TerminTemplate existingTerminTemplate = findActiveOrInactiveById(id);

    DayOfWeek nextDayOfWeek =
        updateTerminTemplateRequestDTO.getDayOfWeek() != null
            ? updateTerminTemplateRequestDTO.getDayOfWeek()
            : existingTerminTemplate.getDayOfWeek();
    LocalTime nextStartTime =
        updateTerminTemplateRequestDTO.getStartTime() != null
            ? updateTerminTemplateRequestDTO.getStartTime()
            : existingTerminTemplate.getStartTime();
    LocalTime nextEndTime =
        updateTerminTemplateRequestDTO.getEndTime() != null
            ? updateTerminTemplateRequestDTO.getEndTime()
            : existingTerminTemplate.getEndTime();

    ensureValidTimeRange(nextStartTime, nextEndTime);
    ensureNoOverlap(nextDayOfWeek, nextStartTime, nextEndTime, id);

    patchMapper.applyPatch(updateTerminTemplateRequestDTO, existingTerminTemplate);
    return toDto(repository.save(existingTerminTemplate));
  }

  @Transactional
  public MessageResponseDTO deleteTerminTemplate(Long id) {
    TerminTemplate terminTemplate = findActiveOrInactiveById(id);
    terminTemplate.setStatus(Status.DELETED);
    repository.save(terminTemplate);
    return new MessageResponseDTO("Uspešno obrisan šablon termina za ID: " + id);
  }

  private void ensureValidTimeRange(LocalTime startTime, LocalTime endTime) {
    if (!endTime.isAfter(startTime)) {
      throw new InvalidTerminTemplateTimeRangeException();
    }
  }

  private void ensureNoOverlap(
      DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Long currentTemplateId) {
    boolean overlaps =
        repository.findAllByDayOfWeekAndStatus(dayOfWeek, Status.ACTIVE).stream()
            .filter(other -> currentTemplateId == null || !other.getId().equals(currentTemplateId))
            .anyMatch(
                other ->
                    startTime.isBefore(other.getEndTime())
                        && other.getStartTime().isBefore(endTime));

    if (overlaps) {
      throw new TerminTemplateOverlapException();
    }
  }

  private TerminTemplate findActiveOrInactiveById(Long id) {
    return repository
        .findByIdAndStatusNot(id, Status.DELETED)
        .orElseThrow(() -> new TerminTemplateNotFoundException(id));
  }

  private TerminTemplateDTO toDto(TerminTemplate terminTemplate) {
    return TerminTemplateDTO.builder()
        .id(terminTemplate.getId())
        .dayOfWeek(terminTemplate.getDayOfWeek())
        .startTime(terminTemplate.getStartTime())
        .endTime(terminTemplate.getEndTime())
        .status(terminTemplate.getStatus())
        .build();
  }
}
