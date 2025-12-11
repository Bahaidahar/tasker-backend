package com.tasktracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasktracker.dto.TaskDTO;
import com.tasktracker.entity.TaskPriority;
import com.tasktracker.entity.TaskStatus;
import com.tasktracker.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        taskDTO = TaskDTO.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllTasks_ShouldReturnTasks() throws Exception {
        when(taskService.getAllTasks()).thenReturn(Arrays.asList(taskDTO));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Test Task"))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void getAllTasks_WhenEmpty_ShouldReturnEmptyArray() throws Exception {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTaskById_WhenExists_ShouldReturnTask() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(taskDTO);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getTaskById_WhenNotExists_ShouldReturn404() throws Exception {
        when(taskService.getTaskById(99L))
                .thenThrow(new EntityNotFoundException("Task not found"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchTasks_WithFilters_ShouldReturnFilteredTasks() throws Exception {
        when(taskService.searchTasks("Test", TaskStatus.TODO, null))
                .thenReturn(Arrays.asList(taskDTO));

        mockMvc.perform(get("/api/tasks/search")
                        .param("search", "Test")
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void searchTasks_WithNoFilters_ShouldReturnAllTasks() throws Exception {
        when(taskService.searchTasks(null, null, null))
                .thenReturn(Arrays.asList(taskDTO));

        mockMvc.perform(get("/api/tasks/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createTask_WithValidData_ShouldReturnCreated() throws Exception {
        TaskDTO newTask = TaskDTO.builder()
                .title("New Task")
                .description("New Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .build();

        when(taskService.createTask(any(TaskDTO.class))).thenReturn(taskDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void createTask_WithoutTitle_ShouldReturn400() throws Exception {
        TaskDTO invalidTask = TaskDTO.builder()
                .description("No title")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTask)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_WithBlankTitle_ShouldReturn400() throws Exception {
        TaskDTO invalidTask = TaskDTO.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTask)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_WhenExists_ShouldReturnUpdated() throws Exception {
        TaskDTO updateDTO = TaskDTO.builder()
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .build();

        when(taskService.updateTask(eq(1L), any(TaskDTO.class))).thenReturn(updateDTO);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"));
    }

    @Test
    void updateTask_WhenNotExists_ShouldReturn404() throws Exception {
        when(taskService.updateTask(eq(99L), any(TaskDTO.class)))
                .thenThrow(new EntityNotFoundException("Task not found"));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(1L);
    }

    @Test
    void deleteTask_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new EntityNotFoundException("Task not found"))
                .when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportToExcel_ShouldReturnXlsxFile() throws Exception {
        byte[] excelContent = new byte[]{1, 2, 3, 4};
        when(taskService.exportToExcel(null, null, null)).thenReturn(excelContent);

        mockMvc.perform(get("/api/tasks/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportToExcel_WithFilters_ShouldReturnFilteredXlsx() throws Exception {
        byte[] excelContent = new byte[]{1, 2, 3, 4};
        when(taskService.exportToExcel("Test", TaskStatus.DONE, TaskPriority.HIGH))
                .thenReturn(excelContent);

        mockMvc.perform(get("/api/tasks/export")
                        .param("search", "Test")
                        .param("status", "DONE")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk());
    }
}
