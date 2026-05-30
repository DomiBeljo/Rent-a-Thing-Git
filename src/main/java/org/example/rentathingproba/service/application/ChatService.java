package org.example.rentathingproba.service.application;

import org.example.rentathingproba.entities.*;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.ChatMessageRepository;
import org.example.rentathingproba.repository.ConversationRepository;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.responses.ChatMessageResponseDTO;
import org.example.rentathingproba.responses.ConversationResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ListingRepository listingRepository;

    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       ListingRepository listingRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
    }

    public ConversationResponseDTO getOrCreateConversation(Long listingId, User requestingUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        // Prevent owners from messaging themselves
        if (listing.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException();
        }

        Conversation conversation = conversationRepository
                .findByListingIdAndBuyerId(listingId, requestingUser.getId())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setListing(listing);
                    c.setBuyer(requestingUser);
                    log.info("Creating new conversation: listingId={}, buyerId={}",
                            listingId, requestingUser.getId());
                    return conversationRepository.save(c);
                });

        return toConversationResponse(conversation, requestingUser);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getMyConversations(User user) {
        return conversationRepository.findAllForUser(user.getId())
                .stream()
                .map(c -> toConversationResponse(c, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getMessages(Long conversationId, User requestingUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        assertParticipant(conversation, requestingUser);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    public ChatMessageResponseDTO sendMessage(Long conversationId, String content, User sender) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        assertParticipant(conversation, sender);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());

        ChatMessage saved = messageRepository.save(message);
        log.info("Message sent: conversationId={}, senderId={}", conversationId, sender.getId());
        return toMessageResponse(saved);
    }

    public void markRead(Long conversationId, User reader) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        assertParticipant(conversation, reader);
        messageRepository.markAllReadInConversation(conversationId, reader.getId());
    }


    private void assertParticipant(Conversation conversation, User user) {
        boolean isBuyer = conversation.getBuyer().getId().equals(user.getId());
        boolean isOwner = conversation.getListing().getUser().getId().equals(user.getId());
        if (!isBuyer && !isOwner) {
            throw new UnauthorizedException();
        }
    }

    private ConversationResponseDTO toConversationResponse(Conversation c, User viewer) {
        boolean viewerIsBuyer = c.getBuyer().getId().equals(viewer.getId());
        User other = viewerIsBuyer ? c.getListing().getUser() : c.getBuyer();

        List<ChatMessage> msgs = c.getMessages();
        String lastMsg = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getContent();
        java.time.LocalDateTime lastAt = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getSentAt();

        long unread = messageRepository
                .countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), viewer.getId());

        return new ConversationResponseDTO(
                c.getId(),
                other.getId(),
                other.getDisplayName(),
                c.getListing().getId(),
                c.getListing().getThings().getName(),
                lastMsg,
                lastAt,
                unread
        );
    }

    private ChatMessageResponseDTO toMessageResponse(ChatMessage m) {
        return new ChatMessageResponseDTO(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getDisplayName(),
                m.getContent(),
                m.getSentAt(),
                m.isRead()
        );
    }
}
