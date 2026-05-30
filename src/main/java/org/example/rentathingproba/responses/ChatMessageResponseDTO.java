package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponseDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;
    private String type;
    private BookingDetailsDTO bookingDetails;
}