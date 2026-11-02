package com.antony.todo.service;

import com.antony.todo.database.model.ToDoEntity;
import com.antony.todo.database.model.UserEntity;
import com.antony.todo.database.repository.ToDoRepository;
import com.antony.todo.database.repository.UserRepository;
import com.antony.todo.exceptions.NotFoundErrorException;
import com.antony.todo.exceptions.UnauthorizedErrorException;
import com.antony.todo.security.UserPrincipal;
import com.antony.todo.utils.MessageDto;
import com.antony.todo.utils.ToDoDto;
import com.antony.todo.utils.ToDoResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToDoServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final String TODO_ID = "todo-1";

    @Mock
    private ToDoRepository toDoRepository;

    @Mock
    private UserRepository userRepository;

    private ToDoService toDoService;

    @BeforeEach
    void setUp() {
        toDoService = new ToDoService(toDoRepository, userRepository);

        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getEmail()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ToDoEntity todoOwnedBy(String ownerId) {
        return new ToDoEntity("Title", "Description", LocalDateTime.now(), ownerId);
    }

    // createToDo

    @Test
    void createToDo_savesEntityWithAuthenticatedUserAsOwner() {
        MessageDto result = toDoService.createToDo(new ToDoDto("Buy milk", "2 liters"));

        ArgumentCaptor<ToDoEntity> captor = ArgumentCaptor.forClass(ToDoEntity.class);
        verify(toDoRepository).save(captor.capture());
        ToDoEntity saved = captor.getValue();
        assertEquals("Buy milk", saved.getTitle());
        assertEquals("2 liters", saved.getDescription());
        assertEquals(USER_ID, saved.getOwnerId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(HttpStatus.CREATED, result.getStatus());
        assertEquals("To-do created successfully", result.getMessage());
    }

    // deleteToDo

    @Test
    void deleteToDo_activeToDo_isSoftDeleted() {
        ToDoEntity toDo = todoOwnedBy(USER_ID);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        MessageDto result = toDoService.deleteToDo(TODO_ID);

        assertFalse(toDo.getActive());
        verify(toDoRepository).save(toDo);
        verify(toDoRepository, never()).delete(any());
        assertEquals(HttpStatus.OK, result.getStatus());
        assertEquals("To-do moved to trash", result.getMessage());
    }

    @Test
    void deleteToDo_inactiveToDo_isHardDeleted() {
        ToDoEntity toDo = todoOwnedBy(USER_ID);
        toDo.setActive(false);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        MessageDto result = toDoService.deleteToDo(TODO_ID);

        verify(toDoRepository).delete(toDo);
        verify(toDoRepository, never()).save(any());
        assertEquals(HttpStatus.OK, result.getStatus());
        assertEquals("To-do deleted successfully", result.getMessage());
    }

    @Test
    void deleteToDo_notFound_throwsNotFound() {
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundErrorException.class, () -> toDoService.deleteToDo(TODO_ID));
        verify(toDoRepository, never()).save(any());
        verify(toDoRepository, never()).delete(any());
    }

    @Test
    void deleteToDo_otherOwner_throwsUnauthorized() {
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(todoOwnedBy(OTHER_USER_ID)));

        assertThrows(UnauthorizedErrorException.class, () -> toDoService.deleteToDo(TODO_ID));
        verify(toDoRepository, never()).save(any());
        verify(toDoRepository, never()).delete(any());
    }

    // getToDosPage

    @Test
    void getToDosPage_mapsEntitiesToResponseDtos() {
        ToDoEntity toDo = todoOwnedBy(USER_ID);
        ReflectionTestUtils.setField(toDo, "id", TODO_ID);
        when(toDoRepository.findByOwnerId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(toDo)));

        Page<ToDoResponseDto> page = toDoService.getToDosPage(0, 10);

        assertEquals(1, page.getTotalElements());
        ToDoResponseDto dto = page.getContent().get(0);
        assertEquals(TODO_ID, dto.getId());
        assertEquals(toDo.getTitle(), dto.getTitle());
        assertEquals(toDo.getDescription(), dto.getDescription());
        assertEquals(toDo.getCreatedAt(), dto.getCreatedAt());
        assertFalse(dto.isDone());
        assertTrue(dto.getActive());
    }

    @Test
    void getToDosPage_usesPageSizeAndAscendingCreatedAtSort() {
        when(toDoRepository.findByOwnerId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        toDoService.getToDosPage(2, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(toDoRepository).findByOwnerId(eq(USER_ID), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("createdAt").isAscending());
    }

    // changeIsDoneStatus

    @Test
    void changeIsDoneStatus_togglesStatus() {
        ToDoEntity toDo = todoOwnedBy(USER_ID);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        MessageDto first = toDoService.changeIsDoneStatus(TODO_ID);
        assertTrue(toDo.getDone());
        assertEquals("To-do 'Done' status changed to: true", first.getMessage());

        MessageDto second = toDoService.changeIsDoneStatus(TODO_ID);
        assertFalse(toDo.getDone());
        assertEquals("To-do 'Done' status changed to: false", second.getMessage());
        assertEquals(HttpStatus.OK, second.getStatus());
        verify(toDoRepository, times(2)).save(toDo);
    }

    @Test
    void changeIsDoneStatus_notFound_throwsNotFound() {
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundErrorException.class, () -> toDoService.changeIsDoneStatus(TODO_ID));
    }

    @Test
    void changeIsDoneStatus_otherOwner_throwsUnauthorized() {
        ToDoEntity toDo = todoOwnedBy(OTHER_USER_ID);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        assertThrows(UnauthorizedErrorException.class, () -> toDoService.changeIsDoneStatus(TODO_ID));
        assertFalse(toDo.getDone());
        verify(toDoRepository, never()).save(any());
    }

    // updateToDo

    @Test
    void updateToDo_updatesTitleAndDescription() {
        ToDoEntity toDo = todoOwnedBy(USER_ID);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        MessageDto result = toDoService.updateToDo(TODO_ID, new ToDoDto("New title", "New description"));

        assertEquals("New title", toDo.getTitle());
        assertEquals("New description", toDo.getDescription());
        verify(toDoRepository).save(toDo);
        assertEquals(HttpStatus.OK, result.getStatus());
        assertEquals("To-do updated successfully", result.getMessage());
    }

    @Test
    void updateToDo_notFound_throwsNotFound() {
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundErrorException.class,
                () -> toDoService.updateToDo(TODO_ID, new ToDoDto("t", "d")));
    }

    @Test
    void updateToDo_otherOwner_throwsUnauthorized() {
        ToDoEntity toDo = todoOwnedBy(OTHER_USER_ID);
        when(toDoRepository.findById(TODO_ID)).thenReturn(Optional.of(toDo));

        assertThrows(UnauthorizedErrorException.class,
                () -> toDoService.updateToDo(TODO_ID, new ToDoDto("Hacked", "Hacked")));
        assertEquals("Title", toDo.getTitle());
        verify(toDoRepository, never()).save(any());
    }
}
