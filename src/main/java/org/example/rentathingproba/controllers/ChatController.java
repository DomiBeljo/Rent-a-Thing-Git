package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.CreateConversationDTO;
import org.example.rentathingproba.dto.SendMessageDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ChatMessageResponseDTO;
import org.example.rentathingproba.responses.ConversationResponseDTO;
import org.example.rentathingproba.service.application.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDTO> getOrCreate(
            @RequestBody CreateConversationDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                chatService.getOrCreateConversation(dto.getListingId(), currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDTO>> myConversations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getMyConversations(currentUser));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getMessages(id, currentUser));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.sendMessage(id, dto.getContent(), currentUser));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        chatService.markRead(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
