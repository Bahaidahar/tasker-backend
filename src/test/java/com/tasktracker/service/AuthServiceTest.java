package com.tasktracker.service;

import com.tasktracker.dto.AuthRequest;
import com.tasktracker.dto.AuthResponse;
import com.tasktracker.dto.RegisterRequest;
import com.tasktracker.entity.User;
import com.tasktracker.repository.UserRepository;
import com.tasktracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void register_WithValidData_ShouldReturnAuthResponse() {
            // Arrange
            RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("Test User", response.getName());

            // Verify password was encoded
            verify(passwordEncoder).encode("password123");

            // Verify user was saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("encodedPassword", userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_WithExistingEmail_ShouldThrowException() {
            // Arrange
            RegisterRequest request = new RegisterRequest("Test User", "existing@example.com", "password123");
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(request));

            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void login_WithValidCredentials_ShouldReturnAuthResponse() {
            // Arrange
            AuthRequest request = new AuthRequest("test@example.com", "password123");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("jwt-token");

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("Test User", response.getName());

            // Verify authentication was attempted
            verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("test@example.com", "password123")
            );
        }

        @Test
        @DisplayName("Should throw exception when credentials are invalid")
        void login_WithInvalidCredentials_ShouldThrowException() {
            // Arrange
            AuthRequest request = new AuthRequest("test@example.com", "wrongPassword");

            when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class,
                () -> authService.login(request));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void login_WithNonExistentUser_ShouldThrowException() {
            // Arrange
            AuthRequest request = new AuthRequest("notfound@example.com", "password123");

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class,
                () -> authService.login(request));
        }
    }
}
