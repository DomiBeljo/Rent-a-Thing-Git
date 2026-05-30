package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConversationResponseDTO {
    private Long conversationId;
    private Long otherUserId;
    private String otherUserName;
    private Long listingId;
    private String listingName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private BookingDetailsDTO activeBooking;
}