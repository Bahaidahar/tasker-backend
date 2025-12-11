package com.tasktracker.service;

import com.tasktracker.dto.TaskDTO;
import com.tasktracker.entity.Task;
import com.tasktracker.entity.TaskPriority;
import com.tasktracker.entity.TaskStatus;
import com.tasktracker.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskDTO = TaskDTO.builder()
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .build();
    }

    @Test
    void getAllTasks_ShouldReturnAllTasks() {
        when(taskRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(task));

        List<TaskDTO> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
        verify(taskRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getTaskById_WhenExists_ShouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskDTO result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals(TaskStatus.TODO, result.getStatus());
    }

    @Test
    void getTaskById_WhenNotExists_ShouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            taskService.getTaskById(99L)
        );
    }

    @Test
    void searchTasks_ShouldReturnFilteredTasks() {
        when(taskRepository.searchTasks("Test", TaskStatus.TODO, null))
                .thenReturn(Arrays.asList(task));

        List<TaskDTO> result = taskService.searchTasks("Test", TaskStatus.TODO, null);

        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
    }

    @Test
    void createTask_ShouldCreateAndReturnTask() {
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskDTO result = taskService.createTask(taskDTO);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_WithNullStatus_ShouldDefaultToTodo() {
        taskDTO.setStatus(null);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskDTO result = taskService.createTask(taskDTO);

        assertNotNull(result);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_WhenExists_ShouldUpdateAndReturnTask() {
        TaskDTO updateDTO = TaskDTO.builder()
                .title("Updated Task")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .build();

        Task updatedTask = Task.builder()
                .id(1L)
                .title("Updated Task")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskDTO result = taskService.updateTask(1L, updateDTO);

        assertEquals("Updated Task", result.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        assertEquals(TaskPriority.HIGH, result.getPriority());
    }

    @Test
    void updateTask_WhenNotExists_ShouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            taskService.updateTask(99L, taskDTO)
        );
    }

    @Test
    void deleteTask_WhenExists_ShouldDelete() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);

        assertDoesNotThrow(() -> taskService.deleteTask(1L));
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTask_WhenNotExists_ShouldThrowException() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () ->
            taskService.deleteTask(99L)
        );
    }

    @Test
    void exportToExcel_ShouldReturnByteArray() throws Exception {
        when(taskRepository.searchTasks(null, null, null))
                .thenReturn(Arrays.asList(task));

        byte[] result = taskService.exportToExcel(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportToExcel_WithEmptyList_ShouldReturnValidExcel() throws Exception {
        when(taskRepository.searchTasks(null, null, null))
                .thenReturn(Arrays.asList());

        byte[] result = taskService.exportToExcel(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
