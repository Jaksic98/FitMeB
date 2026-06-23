package com.consi.fitme.service;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.UpdatePilatesRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.pilates.PilatesNotFoundException;
import com.consi.fitme.mapper.PilatesPatchMapper;
import com.consi.fitme.model.Status;
import com.consi.fitme.model.entity.Pilates;
import com.consi.fitme.repository.PilatesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PilatesService {

  private final PilatesRepository repository;
  private final PilatesPatchMapper patchMapper;

  public List<PilatesDTO> getAllPilates() {
    return repository.findAllByStatusNot(Status.DELETED).stream().map(this::toDto).toList();
  }

  public PilatesDTO getPilates(Long id) {
    return toDto(findActiveOrInactiveById(id));
  }

  @Transactional
  public PilatesDTO createPilates(CreatePilatesRequestDTO createPilatesRequestDTO) {
    Pilates pilates =
        Pilates.builder()
            .position(createPilatesRequestDTO.getPosition())
            .name(createPilatesRequestDTO.getName())
            .build();
    return toDto(repository.save(pilates));
  }

  @Transactional
  public PilatesDTO updatePilates(Long id, UpdatePilatesRequestDTO updatePilatesRequestDTO) {
    Pilates existingPilates = findActiveOrInactiveById(id);
    patchMapper.applyPatch(updatePilatesRequestDTO, existingPilates);
    return toDto(repository.save(existingPilates));
  }

  @Transactional
  public MessageResponseDTO deletePilates(Long id) {
    Pilates pilates = findActiveOrInactiveById(id);
    pilates.setStatus(Status.DELETED);
    repository.save(pilates);
    return new MessageResponseDTO("Uspešno obrisana sprava za pilates za ID: " + id);
  }

  private Pilates findActiveOrInactiveById(Long id) {
    return repository
        .findByIdAndStatusNot(id, Status.DELETED)
        .orElseThrow(() -> new PilatesNotFoundException(id));
  }

  private PilatesDTO toDto(Pilates pilates) {
    return PilatesDTO.builder()
        .id(pilates.getId())
        .position(pilates.getPosition())
        .name(pilates.getName())
        .status(pilates.getStatus())
        .build();
  }
}
