package com.tasktracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasktracker.dto.AuthRequest;
import com.tasktracker.dto.AuthResponse;
import com.tasktracker.dto.RegisterRequest;
import com.tasktracker.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("Should register user and return 201 with token")
        void register_WithValidData_ShouldReturn201() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");
            AuthResponse response = new AuthResponse("jwt-token", "john@example.com", "John Doe");

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        @DisplayName("Should return 400 when name is empty")
        void register_WithEmptyName_ShouldReturn400() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("", "john@example.com", "password123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void register_WithInvalidEmail_ShouldReturn400() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("John Doe", "invalid-email", "password123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void register_WithShortPassword_ShouldReturn400() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void register_WithExistingEmail_ShouldReturn400() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("John Doe", "existing@example.com", "password123");

            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should login user and return 200 with token")
        void login_WithValidCredentials_ShouldReturn200() throws Exception {
            // Arrange
            AuthRequest request = new AuthRequest("john@example.com", "password123");
            AuthResponse response = new AuthResponse("jwt-token", "john@example.com", "John Doe");

            when(authService.login(any(AuthRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
            // Arrange
            AuthRequest request = new AuthRequest("john@example.com", "wrongPassword");

            when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 400 when email is empty")
        void login_WithEmptyEmail_ShouldReturn400() throws Exception {
            // Arrange
            AuthRequest request = new AuthRequest("", "password123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is empty")
        void login_WithEmptyPassword_ShouldReturn400() throws Exception {
            // Arrange
            AuthRequest request = new AuthRequest("john@example.com", "");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}
