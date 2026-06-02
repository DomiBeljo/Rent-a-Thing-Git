package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.entities.*;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.BookingRepository;
import org.example.rentathingproba.repository.ChatMessageRepository;
import org.example.rentathingproba.repository.ConversationRepository;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.responses.ChatMessageResponseDTO;
import org.example.rentathingproba.responses.ConversationResponseDTO;
import org.example.rentathingproba.service.BookingService;
import org.example.rentathingproba.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;

    @InjectMocks
    private ChatService chatService;

    private User owner;
    private User buyer;
    private User stranger;
    private Thing thing;
    private Listing listing;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").enabled(true).build();
        buyer = User.builder().id(2L).username("buyer").email("buyer@example.com").enabled(true).build();
        stranger = User.builder().id(99L).username("stranger").email("s@example.com").enabled(true).build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setDescription("A drill");

        listing = new Listing();
        listing.setId(100L);
        listing.setUser(owner);
        listing.setThings(thing);

        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setListing(listing);
        conversation.setBuyer(buyer);
        conversation.setMessages(new ArrayList<>());
    }


    @Test
    @DisplayName("getOrCreateConversation: throws ListingNotFoundException when listing does not exist")
    void getOrCreate_throwsWhenListingNotFound() {
        when(listingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getOrCreateConversation(100L, buyer))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("getOrCreateConversation: throws UnauthorizedException when owner tries to start conversation with own listing")
    void getOrCreate_throwsWhenOwnerRequestsOwnListing() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> chatService.getOrCreateConversation(100L, owner))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getOrCreateConversation: returns existing conversation when one already exists")
    void getOrCreate_returnsExistingConversation() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        ConversationResponseDTO result = chatService.getOrCreateConversation(100L, buyer);

        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(1L);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateConversation: creates and saves new conversation when none exists")
    void getOrCreate_createsNewConversation() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        ConversationResponseDTO result = chatService.getOrCreateConversation(100L, buyer);

        assertThat(result).isNotNull();
        verify(conversationRepository).save(any(Conversation.class));
    }


    @Test
    @DisplayName("getMyConversations: returns all conversations for the user")
    void getMyConversations_returnsConversations() {
        when(conversationRepository.findAllForUser(2L)).thenReturn(List.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(2L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        List<ConversationResponseDTO> result = chatService.getMyConversations(buyer);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnreadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getMessages: throws RuntimeException when conversation does not exist")
    void getMessages_throwsWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(999L, buyer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    @DisplayName("getMessages: throws UnauthorizedException when non-participant requests messages")
    void getMessages_throwsForNonParticipant() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.getMessages(1L, stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getMessages: returns messages when requested by buyer")
    void getMessages_returnsMessagesForBuyer() {
        ChatMessage msg = buildTextMessage();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, buyer);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("getMessages: returns messages when requested by listing owner")
    void getMessages_returnsMessagesForOwner() {
        ChatMessage msg = buildTextMessage();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, owner);

        assertThat(result).hasSize(1);
    }


    @Test
    @DisplayName("sendMessage: throws UnauthorizedException when non-participant tries to send message")
    void sendMessage_throwsForNonParticipant() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.sendMessage(1L, "Hi!", stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("sendMessage: saves and returns message when sender is a participant")
    void sendMessage_savesAndReturnsMessage() {
        ChatMessage msg = buildTextMessage();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(msg);

        ChatMessageResponseDTO result = chatService.sendMessage(1L, "Hello!", buyer);

        assertThat(result.getContent()).isEqualTo("Hello!");
        assertThat(result.getSenderId()).isEqualTo(2L);
        verify(messageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage: trims whitespace from content before saving")
    void sendMessage_trimsContent() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            assertThat(m.getContent()).isEqualTo("Trimmed");
            return m;
        });

        chatService.sendMessage(1L, "  Trimmed  ", buyer);
    }

    @Test
    @DisplayName("markRead: throws UnauthorizedException when non-participant tries to mark read")
    void markRead_throwsForNonParticipant() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.markRead(1L, stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("markRead: calls repository to mark messages as read")
    void markRead_callsRepository() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        chatService.markRead(1L, buyer);

        verify(messageRepository).markAllReadInConversation(1L, 2L);
    }

    private ChatMessage buildTextMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setConversation(conversation);
        msg.setSender(buyer);
        msg.setContent("Hello!");
        msg.setType(ChatMessage.MessageType.TEXT);
        msg.setSentAt(LocalDateTime.now());
        return msg;
    }
}