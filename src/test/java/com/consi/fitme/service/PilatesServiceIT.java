package com.consi.fitme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.consi.fitme.dto.PilatesDTO;
import com.consi.fitme.dto.request.CreatePilatesRequestDTO;
import com.consi.fitme.dto.request.UpdatePilatesRequestDTO;
import com.consi.fitme.dto.response.MessageResponseDTO;
import com.consi.fitme.exception.pilates.PilatesNotFoundException;
import com.consi.fitme.model.Status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class PilatesServiceIT {

  @Autowired private PilatesService service;

  @Test
  void givenNewPilates_whenCreated_thenStatusDefaultsToActive() {
    String seed = String.valueOf(System.currentTimeMillis());

    PilatesDTO createdPilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("A1." + seed).name("Reformer").build());

    assertThat(createdPilates.getId()).isNotNull();
    assertThat(createdPilates.getPosition()).isEqualTo("A1." + seed);
    assertThat(createdPilates.getName()).isEqualTo("Reformer");
    assertThat(createdPilates.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void givenExistingPilatesId_whenGetPilates_thenReturnsPilatesDetails() {
    String seed = String.valueOf(System.currentTimeMillis());

    PilatesDTO createdPilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("B1." + seed).name("Reformer").build());

    PilatesDTO fetchedPilates = service.getPilates(createdPilates.getId());

    assertThat(fetchedPilates.getId()).isEqualTo(createdPilates.getId());
    assertThat(fetchedPilates.getPosition()).isEqualTo("B1." + seed);
  }

  @Test
  void givenNonExistingPilatesId_whenGetPilates_thenThrowsPilatesNotFoundException() {
    assertThatThrownBy(() -> service.getPilates(Long.MAX_VALUE))
        .isInstanceOf(PilatesNotFoundException.class);
  }

  @Test
  void givenExistingPilates_whenUpdateWithPartialFields_thenUpdatesOnlyProvidedFields() {
    String seed = String.valueOf(System.currentTimeMillis());

    PilatesDTO createdPilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("C1." + seed).name("Reformer").build());

    PilatesDTO updatedPilates =
        service.updatePilates(
            createdPilates.getId(), UpdatePilatesRequestDTO.builder().name("Cadillac").build());

    assertThat(updatedPilates.getName()).isEqualTo("Cadillac");
    assertThat(updatedPilates.getPosition()).isEqualTo("C1." + seed);
    assertThat(updatedPilates.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void givenExistingPilates_whenDeletePilates_thenPerformsSoftDeleteAndIsNotAccessible() {
    String seed = String.valueOf(System.currentTimeMillis());

    PilatesDTO createdPilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("D1." + seed).name("Reformer").build());

    MessageResponseDTO response = service.deletePilates(createdPilates.getId());

    assertThat(response.getMessage()).contains("Uspešno obrisana sprava za pilates");
    Long deletedPilatesId = createdPilates.getId();
    assertThatThrownBy(() -> service.getPilates(deletedPilatesId))
        .isInstanceOf(PilatesNotFoundException.class);
  }

  @Test
  void givenDeletedPilates_whenGetAllPilates_thenExcludesDeletedPilates() {
    String seed = String.valueOf(System.currentTimeMillis());

    PilatesDTO activePilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("E1." + seed).name("Reformer").build());
    PilatesDTO toDeletePilates =
        service.createPilates(
            CreatePilatesRequestDTO.builder().position("E2." + seed).name("Reformer").build());

    service.deletePilates(toDeletePilates.getId());

    List<PilatesDTO> allPilates = service.getAllPilates();

    assertThat(allPilates).extracting(PilatesDTO::getId).contains(activePilates.getId());
    assertThat(allPilates).extracting(PilatesDTO::getId).doesNotContain(toDeletePilates.getId());
  }
}
