package org.example.rentathingproba.notification;

import org.springframework.stereotype.Component;

@Component
public class ListingEmailTemplate {

    public String buildCreateHtml(String username, Long listingId) {
        return """
                <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: auto; background: white; padding: 30px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Rent-a-Thing</h2>
                <p style="font-size: 16px; color: #555;">Hello <b>%s</b>,</p>
                <p style="font-size: 16px; color: #555;">
                    Your listing <b>#%d</b> has been successfully created and is now live.
                </p>
                <p style="font-size: 14px; color: #999;">Happy renting!</p>
                </div>
                </div>
                """.formatted(username, listingId);
    }

    public String buildUpdateHtml(String username, Long listingId) {
        return """
                <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: auto; background: white; padding: 30px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Rent-a-Thing</h2>
                <p style="font-size: 16px; color: #555;">Hello <b>%s</b>,</p>
                <p style="font-size: 16px; color: #555;">
                    Your listing <b>#%d</b> has been updated successfully.
                </p>
                <p style="font-size: 14px; color: #999;">The changes are now visible to other users.</p>
                </div>
                </div>
                """.formatted(username, listingId);
    }

    public String buildDeleteHtml(String username, Long listingId) {
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
