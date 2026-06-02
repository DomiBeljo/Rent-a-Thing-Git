package org.example.rentathingproba.integration;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.ChatMessageResponseDTO;
import org.example.rentathingproba.responses.ConversationResponseDTO;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.service.application.ChatService;
import org.example.rentathingproba.service.application.ListingService;
import org.example.rentathingproba.service.application.ThingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("ChatService Integration Tests")
class ChatServiceIntegrationTest {

    @Autowired private ChatService chatService;
    @Autowired private ThingService thingService;
    @Autowired private ListingService listingService;
    @Autowired private UserRepository userRepository;

    private User owner;
    private User buyer;
    private ListingResponseDTO listing;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .username("chat_owner_int")
                .email("chat_owner_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        buyer = userRepository.save(User.builder()
                .username("chat_buyer_int")
                .email("chat_buyer_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        ThingDTO thingDto = new ThingDTO();
        thingDto.setName("Ladder");
        thingDto.setCategory("Tools");
        thingDto.setDescription("A tall ladder");
        thingDto.setImageUrls(List.of("img.jpg"));
        ThingResponseDTO thing = thingService.createThing(thingDto, owner);

        ListingDTO listingDto = new ListingDTO();
        listingDto.setThingId(thing.getThingId());
        listingDto.setPrice(BigDecimal.valueOf(10.00));
        listingDto.setLocation("Zagreb");
        listingDto.setSecurityDeposit(BigDecimal.valueOf(30.00));
        listing = listingService.createListing(listingDto, owner);
    }

    @Test
    @DisplayName("getOrCreateConversation: creates a new conversation in the database")
    void getOrCreate_createsConversation() {
        ConversationResponseDTO result = chatService.getOrCreateConversation(listing.getListingId(), buyer);

        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isNotNull();
        assertThat(result.getListingId()).isEqualTo(listing.getListingId());
        assertThat(result.getOtherUserId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("getOrCreateConversation: returns the same conversation on repeated calls (idempotent)")
    void getOrCreate_isIdempotent() {
        ConversationResponseDTO first = chatService.getOrCreateConversation(listing.getListingId(), buyer);
        ConversationResponseDTO second = chatService.getOrCreateConversation(listing.getListingId(), buyer);

        assertThat(first.getConversationId()).isEqualTo(second.getConversationId());
    }

    @Test
    @DisplayName("getOrCreateConversation: throws UnauthorizedException when owner opens own listing's chat")
    void getOrCreate_throwsForOwner() {
        assertThatThrownBy(() -> chatService.getOrCreateConversation(listing.getListingId(), owner))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getMyConversations: returns conversation for buyer after creation")
    void getMyConversations_returnsBuyerConversation() {
        chatService.getOrCreateConversation(listing.getListingId(), buyer);

        List<ConversationResponseDTO> conversations = chatService.getMyConversations(buyer);

        assertThat(conversations).isNotEmpty();
        assertThat(conversations).anyMatch(c -> c.getListingId().equals(listing.getListingId()));
    }

    @Test
    @DisplayName("getMyConversations: returns conversation for owner after creation")
    void getMyConversations_returnsOwnerConversation() {
        chatService.getOrCreateConversation(listing.getListingId(), buyer);

        List<ConversationResponseDTO> conversations = chatService.getMyConversations(owner);

        assertThat(conversations).isNotEmpty();
    }

    @Test
    @DisplayName("sendMessage: persists message and it appears in getMessages")
    void sendMessage_persistsAndAppearsInHistory() {
        ConversationResponseDTO conv = chatService.getOrCreateConversation(listing.getListingId(), buyer);
        chatService.sendMessage(conv.getConversationId(), "Hello, is this available?", buyer);

        List<ChatMessageResponseDTO> messages = chatService.getMessages(conv.getConversationId(), buyer);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello, is this available?");
        assertThat(messages.get(0).getSenderId()).isEqualTo(buyer.getId());
    }

    @Test
    @DisplayName("sendMessage: both participants can send messages in the same conversation")
    void sendMessage_bothParticipantsCanSend() {
        ConversationResponseDTO conv = chatService.getOrCreateConversation(listing.getListingId(), buyer);
        chatService.sendMessage(conv.getConversationId(), "Is it available?", buyer);
        chatService.sendMessage(conv.getConversationId(), "Yes, it is!", owner);

        List<ChatMessageResponseDTO> messages = chatService.getMessages(conv.getConversationId(), buyer);

        assertThat(messages).hasSize(2);
    }

    @Test
    @DisplayName("sendMessage: throws UnauthorizedException when non-participant tries to send")
    void sendMessage_throwsForNonParticipant() {
        User stranger = userRepository.save(User.builder()
                .username("chat_stranger_int")
                .email("chat_stranger_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        ConversationResponseDTO conv = chatService.getOrCreateConversation(listing.getListingId(), buyer);

        assertThatThrownBy(() -> chatService.sendMessage(conv.getConversationId(), "hack", stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("markRead: marks unread messages as read for the specified user")
    void markRead_marksMessagesRead() {
        ConversationResponseDTO conv = chatService.getOrCreateConversation(listing.getListingId(), buyer);
        chatService.sendMessage(conv.getConversationId(), "Question here", buyer);
        chatService.markRead(conv.getConversationId(), owner);

        List<ConversationResponseDTO> ownerConversations = chatService.getMyConversations(owner);
        assertThat(ownerConversations).anyMatch(c ->
                c.getConversationId().equals(conv.getConversationId()) && c.getUnreadCount() == 0);
    }
}