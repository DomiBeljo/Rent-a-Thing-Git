package org.example.rentathingproba.email.notification.handler;

import jakarta.mail.MessagingException;
import org.example.rentathingproba.email.central.ListingAction;
import org.example.rentathingproba.email.central.ListingEvent;
import org.example.rentathingproba.service.infrastructure.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeleteListingHandler implements ListingNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(DeleteListingHandler.class);

    private final EmailService emailService;

    public DeleteListingHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public ListingAction supports() {
        return ListingAction.DELETE;
    }

    @Override
    public void handle(ListingEvent event) {
        String subject = "Rent-a-Thing — Your listing has been removed";
        String html = buildHtml(event.getOwnerUsername(), event.getListingId());
        try {
            emailService.sendVerificationEmail(event.getOwnerEmail(), subject, html);
            log.info("Delete-listing notification sent: listingId={}, email={}", event.getListingId(), event.getOwnerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send delete-listing notification: listingId={}, error={}", event.getListingId(), e.getMessage());
        }
    }

    private String buildHtml(String username, Long listingId) {
        return """
                <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: auto; background: white; padding: 30px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Rent-a-Thing</h2>
                <p style="font-size: 16px; color: #555;">Hello <b>%s</b>,</p>
                <p style="font-size: 16px; color: #555;">
                    Your listing <b>#%d</b> has been removed from the platform.
                </p>
                <p style="font-size: 14px; color: #999;">If this was a mistake, please contact support.</p>
                </div>
                </div>
                """.formatted(username, listingId);
    }
}
