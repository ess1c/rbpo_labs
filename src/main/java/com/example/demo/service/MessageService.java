package com.example.demo.service;

import com.example.demo.entity.Listing;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ListingService listingService;
    private final UserService userService;

    public MessageService(MessageRepository messageRepository,
                         ListingService listingService,
                         UserService userService) {
        this.messageRepository = messageRepository;
        this.listingService = listingService;
        this.userService = userService;
    }

    public List<Message> getMessagesByListing(Long listingId) {
        return messageRepository.findByListingIdOrderByCreatedAtAsc(listingId);
    }

    public List<Message> getConversation(Long listingId, Long userId) {
        return messageRepository.findConversationByListingAndUser(listingId, userId);
    }

    public List<Message> getMessagesBySender(Long senderId) {
        return messageRepository.findBySenderId(senderId);
    }

    public List<Message> getMessagesByReceiver(Long receiverId) {
        return messageRepository.findByReceiverId(receiverId);
    }

    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + id));
    }

    @Transactional
    public Message createMessage(Message message, Long listingId, Long senderId, Long receiverId) {
        if (listingId == null || listingId <= 0) {
            throw new IllegalArgumentException("Listing ID не может быть пустым");
        }
        
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("Sender ID не может быть пустым");
        }
        
        if (receiverId == null || receiverId <= 0) {
            throw new IllegalArgumentException("Receiver ID не может быть пустым");
        }
        
        Listing listing = listingService.getListingById(listingId);
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(receiverId);
        
        // Нельзя отправлять сообщение самому себе
        if (senderId.equals(receiverId)) {
            throw new IllegalStateException("Нельзя отправлять сообщение самому себе");
        }
        
        message.setListing(listing);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setIsRead(false);
        
        return messageRepository.save(message);
    }

    @Transactional
    public Message markAsRead(Long id, Long userId) {
        Message message = getMessageById(id);
        
        if (!message.getReceiver().getId().equals(userId)) {
            throw new IllegalStateException("You can only mark your own received messages as read");
        }
        
        message.setIsRead(true);
        return messageRepository.save(message);
    }

    @Transactional
    public Message updateMessage(Long id, String text, Long userId, String userRole) {
        Message message = getMessageById(id);
        
        if (!"ADMIN".equals(userRole) && !message.getSender().getId().equals(userId)) {
            throw new IllegalStateException("You can only update your own messages");
        }
        
        message.setText(text);
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Long id, Long userId, String userRole) {
        Message message = getMessageById(id);
        
        if (!"ADMIN".equals(userRole) && !message.getSender().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own messages");
        }
        
        messageRepository.deleteById(id);
    }
}

