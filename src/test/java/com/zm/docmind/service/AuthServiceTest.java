package com.zm.docmind.service;

import com.zm.docmind.entity.User;
import com.zm.docmind.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("正常注册应返回包含 token 和 email 的 AuthResponse")
        void success() {
            when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPwd");
            when(jwtService.generateToken("user@test.com")).thenReturn("jwt-token");

            var response = authService.register("user@test.com", "password123");

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("user@test.com");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("user@test.com");
            assertThat(saved.getPassword()).isEqualTo("encodedPwd");
        }

        @Test
        @DisplayName("邮箱为 null 应抛 IllegalArgumentException")
        void nullEmail_throwsException() {
            assertThatThrownBy(() -> authService.register(null, "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("邮箱");
        }

        @Test
        @DisplayName("邮箱为空字符串应抛 IllegalArgumentException")
        void blankEmail_throwsException() {
            assertThatThrownBy(() -> authService.register("  ", "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("邮箱");
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "no-at-sign.com", "@no-local.com", "no-domain@", "user@.com"})
        @DisplayName("邮箱格式不正确应抛 IllegalArgumentException")
        void invalidEmailFormat_throwsException(String email) {
            assertThatThrownBy(() -> authService.register(email, "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("邮箱格式");
        }

        @Test
        @DisplayName("密码长度不足 6 位应抛 IllegalArgumentException")
        void shortPassword_throwsException() {
            assertThatThrownBy(() -> authService.register("user@test.com", "12345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("密码");
        }

        @Test
        @DisplayName("密码为 null 应抛 IllegalArgumentException")
        void nullPassword_throwsException() {
            assertThatThrownBy(() -> authService.register("user@test.com", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("密码");
        }

        @Test
        @DisplayName("重复邮箱应抛 IllegalArgumentException")
        void duplicateEmail_throwsException() {
            when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register("user@test.com", "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("已被注册");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("正常登录应返回 AuthResponse")
        void success() {
            User user = User.builder().email("user@test.com").password("encodedPwd").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encodedPwd")).thenReturn(true);
            when(jwtService.generateToken("user@test.com")).thenReturn("jwt-token");

            var response = authService.login("user@test.com", "password123");

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("不存在的邮箱应抛 IllegalArgumentException")
        void nonexistentEmail_throwsException() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login("unknown@test.com", "password123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("邮箱或密码错误");
        }

        @Test
        @DisplayName("密码错误应抛 IllegalArgumentException")
        void wrongPassword_throwsException() {
            User user = User.builder().email("user@test.com").password("encodedPwd").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPwd", "encodedPwd")).thenReturn(false);

            assertThatThrownBy(() -> authService.login("user@test.com", "wrongPwd"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("密码错误");
        }

        @Test
        @DisplayName("登录时 email 应被 trim + toLowerCase")
        void emailNormalization() {
            User user = User.builder().email("user@test.com").password("encodedPwd").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encodedPwd")).thenReturn(true);
            when(jwtService.generateToken("user@test.com")).thenReturn("jwt-token");

            var response = authService.login("  User@Test.COM  ", "password123");

            assertThat(response.getEmail()).isEqualTo("user@test.com");
            verify(userRepository).findByEmail("user@test.com");
        }
    }
}
