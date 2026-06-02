package org.example.rentathingproba.unit.service;
import jakarta.mail.MessagingException;
import org.example.rentathingproba.dto.LoginUserDTO;
import org.example.rentathingproba.dto.RegisteredUserDTO;
import org.example.rentathingproba.dto.VerifiedUserDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.*;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.service.AuthenticationService;
import org.example.rentathingproba.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User enabledUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        enabledUser = User.builder().id(1L)
                .username("dom")
                .email("dom@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build();

        disabledUser = User.builder()
                .id(2L)
                .username("ana")
                .email("ana@example.com")
                .password("encodedPassword")
                .enabled(false)
                .verificationCode("123456")
                .verificationCodeExpiration(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    //SignUp
    @Test
    @DisplayName("signUp: saves user with encoded password and sends verification email")
    void signUp_savesUserAndSendsEmail() throws MessagingException {
        RegisteredUserDTO dto = new RegisteredUserDTO();
        dto.setUsername("dom");
        dto.setEmail("dom@example.com");
        dto.setPassword("plain");

        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        User result = authenticationService.signUp(dto);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getUsername()).isEqualTo("dom@example.com");
        assertThat(result.getEmail()).isEqualTo("dom@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getVerificationCode()).isNotNull();
        assertThat(result.getVerificationCodeExpiration()).isAfter(LocalDateTime.now());

        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("dom@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("signUp: verification code is a 6-digit number")
    void signUp_verificationCodeIsSixDigits() throws MessagingException {
        RegisteredUserDTO dto = new RegisteredUserDTO();
        dto.setUsername("user");
        dto.setEmail("u@example.com");
        dto.setPassword("pass");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authenticationService.signUp(dto);

        assertThat(result.getVerificationCode()).matches("\\d{6}");
    }

    @Test
    @DisplayName("signUp: email failure is silently swallowed (no exception propagated)")
    void signUp_emailFailureDoesNotPropagateException() throws MessagingException {
        RegisteredUserDTO dto = new RegisteredUserDTO();
        dto.setUsername("user");
        dto.setEmail("u@example.com");
        dto.setPassword("pass");

        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(MessagingException.class).when(emailService)
                .sendVerificationEmail(anyString(), anyString(), anyString());

        assertThatNoException().isThrownBy(() -> authenticationService.signUp(dto));
    }

    //Authenticate
    @Test
    @DisplayName("authenticate: returns user on valid credentials for enabled account")
    void authenticate_returnsUserOnSuccess() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("john@example.com");
        dto.setPassword("plain");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(enabledUser));

        User result = authenticationService.authenticate(dto);

        assertThat(result).isEqualTo(enabledUser);
        verify(authenticationManager).authenticate(
                argThat(t -> t instanceof UsernamePasswordAuthenticationToken
                        && Objects.equals(t.getPrincipal(), "john@example.com"))
        );
    }

    @Test
    @DisplayName("authenticate: throws UserNotFoundException when email not found")
    void authenticate_throwsUserNotFoundException() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("missing@example.com");
        dto.setPassword("pass");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(dto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("authenticate: throws AccountNotVerifiedException when account not enabled")
    void authenticate_throwsAccountNotVerifiedExceptionWhenDisabled() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("ana@example.com");
        dto.setPassword("pass");

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> authenticationService.authenticate(dto))
                .isInstanceOf(AccountNotVerifiedException.class);
        verifyNoInteractions(authenticationManager);
    }

    // Verify user
    @Test
    @DisplayName("verifyUser: enables account on correct code before expiration")
    void verifyUser_enablesAccountOnValidCode() {
        VerifiedUserDTO dto = new VerifiedUserDTO();
        dto.setEmail("ana@example.com");
        dto.setVerificationCode("123456");

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.verifyUser(dto);

        assertThat(disabledUser.isEnabled()).isTrue();
        assertThat(disabledUser.getVerificationCode()).isNull();
        assertThat(disabledUser.getVerificationCodeExpiration()).isNull();
        verify(userRepository).save(disabledUser);
    }

    @Test
    @DisplayName("verifyUser: throws UserNotFoundException when email unknown")
    void verifyUser_throwsUserNotFound() {
        VerifiedUserDTO dto = new VerifiedUserDTO();
        dto.setEmail("ghost@example.com");
        dto.setVerificationCode("000000");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.verifyUser(dto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("verifyUser: throws AccountAlreadyVerifiedException when account already enabled")
    void verifyUser_throwsWhenAlreadyEnabled() {
        VerifiedUserDTO dto = new VerifiedUserDTO();
        dto.setEmail("john@example.com");
        dto.setVerificationCode("123456");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(enabledUser));

        assertThatThrownBy(() -> authenticationService.verifyUser(dto))
                .isInstanceOf(AccountAlreadyVerifiedException.class);
    }

    @Test
    @DisplayName("verifyUser: throws VerificationCodeExpiredException when code is expired")
    void verifyUser_throwsOnExpiredCode() {
        disabledUser.setVerificationCodeExpiration(LocalDateTime.now().minusMinutes(1));

        VerifiedUserDTO dto = new VerifiedUserDTO();
        dto.setEmail("ana@example.com");
        dto.setVerificationCode("123456");

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> authenticationService.verifyUser(dto))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    @DisplayName("verifyUser: throws VerificationCodeInvalidException when code does not match")
    void verifyUser_throwsOnWrongCode() {
        VerifiedUserDTO dto = new VerifiedUserDTO();
        dto.setEmail("ana@example.com");
        dto.setVerificationCode("999999");

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> authenticationService.verifyUser(dto))
                .isInstanceOf(VerificationCodeInvalidException.class);
    }

    // Resend verification code
    @Test
    @DisplayName("resendVerificationCode: generates new code and saves for unverified user")
    void resendVerificationCode_generatesNewCodeAndSaves() throws MessagingException {
        String oldCode = disabledUser.getVerificationCode();

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.resendVerificationCode("ana@example.com");

        assertThat(disabledUser.getVerificationCode()).isNotNull();
        assertThat(disabledUser.getVerificationCodeExpiration()).isAfter(LocalDateTime.now());
        verify(userRepository).save(disabledUser);
        verify(emailService).sendVerificationEmail(eq("ana@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("resendVerificationCode: throws UserNotFoundException for unknown email")
    void resendVerificationCode_throwsUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.resendVerificationCode("ghost@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("resendVerificationCode: throws AccountAlreadyVerifiedException for enabled account")
    void resendVerificationCode_throwsWhenAlreadyVerified() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(enabledUser));

        assertThatThrownBy(() -> authenticationService.resendVerificationCode("john@example.com"))
                .isInstanceOf(AccountAlreadyVerifiedException.class);
        verify(userRepository, never()).save(any());
    }
}