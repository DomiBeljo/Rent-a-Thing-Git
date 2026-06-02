package org.example.rentathingproba.service.application;

import jakarta.mail.MessagingException;
import org.example.rentathingproba.dto.LoginUserDTO;
import org.example.rentathingproba.dto.RegisteredUserDTO;
import org.example.rentathingproba.dto.VerifiedUserDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.*;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    public User signUp(RegisteredUserDTO input) {
        log.info("Registering new user: username='{}', email='{}'", input.getUsername(), input.getEmail());

        User user = User.builder()
                .username(input.getUsername())
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .build();
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);

        sendVerificationEmail(user);
        User saved = userRepository.save(user);

        log.info("User registered successfully: id={}", saved.getId());
        return saved;
    }

    public User authenticate(LoginUserDTO input) {
        log.info("Authentication attempt for email='{}'", input.getEmail());

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new UserNotFoundException(input.getEmail()));

        if (!user.isEnabled()) {
            throw new AccountNotVerifiedException();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
        );

        log.info("Authentication successful: userId='{}'", user.getId());
        return user;
    }

    public void verifyUser(VerifiedUserDTO input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new UserNotFoundException(input.getEmail()));

        if (user.isEnabled()) {
            throw new AccountAlreadyVerifiedException();
        }

        if (user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new VerificationCodeExpiredException();
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new VerificationCodeInvalidException();
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiration(null);
        userRepository.save(user);

        log.info("User verified successfully: email='{}'", input.getEmail());
    }

    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.isEnabled()) {
            throw new AccountAlreadyVerifiedException();
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(5));
        sendVerificationEmail(user);
        userRepository.save(user);

        log.info("Verification code resent: email='{}'", email);
    }

    private void sendVerificationEmail(User user) {
        String subject = "Rent-a-Thing — Account Verification";
        String htmlMessage = buildVerificationEmailHtml(user.getUsername(), user.getVerificationCode());
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to '{}': {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(String username, String verificationCode) {
        return """
                <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: auto; background: white; padding: 30px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Rent-a-Thing</h2>
                <p style="font-size: 16px; color: #555;">
                    Hello <b>%s</b>,
                </p>
                <p style="font-size: 16px; color: #555;">
                    Thank you for registering! To complete the process, enter your verification code:
                </p>
                <div style="margin: 30px 0;">
                <span style="display: inline-block; padding: 15px 25px; font-size: 24px; letter-spacing: 5px;
                    background-color: #4CAF50; color: white; border-radius: 8px;">
                    %s
                </span>
                </div>
                <p style="font-size: 14px; color: #999;">
                    This code is valid for 15 minutes.
                </p>
                <hr style="margin: 30px 0;">
                <p style="font-size: 12px; color: #aaa;">
                    If you did not create this account, you can safely ignore this email.
                </p>
                </div>
                </div>
                """.formatted(username, verificationCode);
    }

    private String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}