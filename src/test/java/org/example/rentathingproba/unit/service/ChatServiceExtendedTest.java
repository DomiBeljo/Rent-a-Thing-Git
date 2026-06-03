package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.entities.*;
import org.example.rentathingproba.repository.BookingRepository;
import org.example.rentathingproba.repository.ChatMessageRepository;
import org.example.rentathingproba.repository.ConversationRepository;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.responses.BookingDetailsDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Extended Unit Tests")
class ChatServiceExtendedTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;

    @InjectMocks
    private ChatService chatService;

    private User owner;
    private User buyer;
    private Thing thing;
    private Listing listing;
    private Conversation conversation;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").enabled(true).build();
        buyer = User.builder().id(2L).username("buyer").email("buyer@example.com").enabled(true).build();

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
        listing.setPrice(BigDecimal.valueOf(20));
        listing.setIsAvailable(true);

        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setListing(listing);
        conversation.setBuyer(buyer);
        conversation.setMessages(new ArrayList<>());

        booking = new Booking();
        booking.setId(200L);
        booking.setListing(listing);
        booking.setRenter(buyer);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPricePerDay(BigDecimal.valueOf(20));
        booking.setTotalAmount(BigDecimal.valueOf(110));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(LocalDateTime.now().plusHours(24));
    }

    @Test
    @DisplayName("getMessages: booking request message includes BookingDetailsDTO")
    void getMessages_bookingRequestMessage_includesBookingDetails() {
        ChatMessage msg = new ChatMessage();
        msg.setId(10L);
        msg.setConversation(conversation);
        msg.setSender(buyer);
        msg.setContent("Booking Request: Drill");
        msg.setType(ChatMessage.MessageType.BOOKING_REQUEST);
        msg.setSentAt(LocalDateTime.now());
        msg.setBooking(booking);

        BookingDetailsDTO mockDetails = mock(BookingDetailsDTO.class);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));
        when(bookingService.toBookingDetailsDTO(booking, buyer)).thenReturn(mockDetails);

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, buyer);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("booking_request");
        assertThat(result.get(0).getBookingDetails()).isEqualTo(mockDetails);
        verify(bookingService).toBookingDetailsDTO(booking, buyer);
    }

    @Test
    @DisplayName("getMessages: booking confirmed message includes BookingDetailsDTO")
    void getMessages_bookingConfirmedMessage_includesBookingDetails() {
        ChatMessage msg = new ChatMessage();
        msg.setId(11L);
        msg.setConversation(conversation);
        msg.setSender(owner);
        msg.setContent("Booking Confirmed!");
        msg.setType(ChatMessage.MessageType.BOOKING_CONFIRMED);
        msg.setSentAt(LocalDateTime.now());
        msg.setBooking(booking);

        BookingDetailsDTO mockDetails = mock(BookingDetailsDTO.class);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));
        when(bookingService.toBookingDetailsDTO(booking, owner)).thenReturn(mockDetails);

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookingDetails()).isEqualTo(mockDetails);
    }

    @Test
    @DisplayName("getMessages: non-TEXT message without a booking returns null bookingDetails")
    void getMessages_nonTextMessageWithoutBooking_nullBookingDetails() {
        ChatMessage msg = new ChatMessage();
        msg.setId(12L);
        msg.setConversation(conversation);
        msg.setSender(owner);
        msg.setContent("Booking Declined");
        msg.setType(ChatMessage.MessageType.BOOKING_DECLINED);
        msg.setSentAt(LocalDateTime.now());
        msg.setBooking(null);  // no booking attached

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, buyer);

        assertThat(result.get(0).getBookingDetails()).isNull();
        verify(bookingService, never()).toBookingDetailsDTO(any(), any());
    }

    @Test
    @DisplayName("getMessages: TEXT message with a booking still returns null bookingDetails")
    void getMessages_textMessageWithBooking_nullBookingDetails() {
        ChatMessage msg = new ChatMessage();
        msg.setId(13L);
        msg.setConversation(conversation);
        msg.setSender(buyer);
        msg.setContent("Hello!");
        msg.setType(ChatMessage.MessageType.TEXT);
        msg.setSentAt(LocalDateTime.now());
        msg.setBooking(booking);  // TEXT message should never include booking details

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));

        List<ChatMessageResponseDTO> result = chatService.getMessages(1L, buyer);

        assertThat(result.get(0).getBookingDetails()).isNull();
        verify(bookingService, never()).toBookingDetailsDTO(any(), any());
    }

    @Test
    @DisplayName("getOrCreateConversation: includes active booking details when booking exists for conversation")
    void getOrCreate_includesActiveBookingInResponse() {
        BookingDetailsDTO mockDetails = mock(BookingDetailsDTO.class);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of(booking));
        when(bookingService.toBookingDetailsDTO(booking, buyer)).thenReturn(mockDetails);

        ConversationResponseDTO result = chatService.getOrCreateConversation(100L, buyer);

        assertThat(result.getActiveBooking()).isEqualTo(mockDetails);
        verify(bookingService).toBookingDetailsDTO(booking, buyer);
    }

    @Test
    @DisplayName("getMyConversations: active booking is null when no active booking for conversation")
    void getMyConversations_noActiveBooking_nullInResponse() {
        when(conversationRepository.findAllForUser(2L)).thenReturn(List.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        List<ConversationResponseDTO> result = chatService.getMyConversations(buyer);

        assertThat(result.get(0).getActiveBooking()).isNull();
    }

    @Test
    @DisplayName("getMyConversations: owner sees buyer as the 'other' user in conversation")
    void getMyConversations_ownerPerspective_otherIsBuyer() {
        when(conversationRepository.findAllForUser(1L)).thenReturn(List.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 1L)).thenReturn(1L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        List<ConversationResponseDTO> result = chatService.getMyConversations(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOtherUserId()).isEqualTo(2L);   // buyer id
        assertThat(result.get(0).getUnreadCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getMyConversations: lastMessage and lastAt populated when conversation has messages")
    void getMyConversations_withMessages_lastMsgPopulated() {
        ChatMessage lastMsg = new ChatMessage();
        lastMsg.setSender(buyer);
        lastMsg.setContent("Is this available?");
        lastMsg.setType(ChatMessage.MessageType.TEXT);
        lastMsg.setSentAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        conversation.setMessages(List.of(lastMsg));

        when(conversationRepository.findAllForUser(2L)).thenReturn(List.of(conversation));
        when(messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(1L, 2L)).thenReturn(0L);
        when(bookingRepository.findActiveBookingByConversation(1L)).thenReturn(List.of());

        List<ConversationResponseDTO> result = chatService.getMyConversations(buyer);

        assertThat(result.get(0).getLastMessage()).isEqualTo("Is this available?");
        assertThat(result.get(0).getLastMessageAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    @Test
    @DisplayName("sendMessage: throws RuntimeException when conversation does not exist")
    void sendMessage_throwsWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(999L, "Hello", buyer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    @DisplayName("markRead: throws RuntimeException when conversation does not exist")
    void markRead_throwsWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.markRead(999L, buyer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    @DisplayName("markRead: owner can mark conversation messages as read")
    void markRead_ownerCanMarkRead() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        chatService.markRead(1L, owner);

        verify(messageRepository).markAllReadInConversation(1L, 1L);
    }
}