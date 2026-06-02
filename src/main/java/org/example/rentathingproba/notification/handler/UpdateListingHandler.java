package org.example.rentathingproba.notification.handler;

import jakarta.mail.MessagingException;
import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;
import org.example.rentathingproba.notification.ListingEmailTemplate;
import org.example.rentathingproba.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UpdateListingHandler implements ListingNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(UpdateListingHandler.class);

    private final EmailService emailService;
    private final ListingEmailTemplate template;

    public UpdateListingHandler(EmailService emailService, ListingEmailTemplate template) {
        this.emailService = emailService;
        this.template = template;
    }

    @Override
    public ListingAction supports() {
        return ListingAction.UPDATE;
    }

    @Override
    public void handle(ListingEvent event) {
        String html = template.buildUpdateHtml(event.getOwnerUsername(), event.getListingId());
        try {
            emailService.sendVerificationEmail(event.getOwnerEmail(), "Rent-a-Thing — Your listing has been updated", html);
            log.info("Update-listing notification sent: listingId={}, email={}", event.getListingId(), event.getOwnerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send update-listing notification: listingId={}, error={}", event.getListingId(), e.getMessage());
        }
    }
}
