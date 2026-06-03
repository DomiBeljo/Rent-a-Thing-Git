package org.example.rentathingproba.unit.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.rentathingproba.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendVerificationEmail: creates MimeMessage and sends it via JavaMailSender")
    void sendVerificationEmail_createsAndSendsMessage() throws MessagingException {

        assertThatNoException().isThrownBy(() ->
                emailService.sendVerificationEmail(
                        "user@example.com",
                        "Verify your account",
                        "<p>Your code: 123456</p>"
                )
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendVerificationEmail: propagates MessagingException when MimeMessage setup fails")
    void sendVerificationEmail_propagatesMessagingException() {
        MimeMessage broken = mock(MimeMessage.class, invocation -> {
            throw new jakarta.mail.MessagingException("SMTP failure");
        });
        when(mailSender.createMimeMessage()).thenReturn(broken);

        assertThatThrownBy(() ->
                emailService.sendVerificationEmail("a@b.com", "subj", "<p>body</p>")
        ).isInstanceOf(MessagingException.class);
    }
}