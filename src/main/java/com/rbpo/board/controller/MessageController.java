package com.rbpo.board.controller;

import com.rbpo.board.dto.MessageDTO;
import com.rbpo.board.model.Message;
import com.rbpo.board.repository.MessageRepository;
import com.rbpo.board.repository.UserRepository;
import com.rbpo.board.repository.ListingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository, ListingRepository listingRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<MessageDTO>> getAllMessages() {
        List<Message> messages = messageRepository.findAll();
        List<MessageDTO> messageDTOs = messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messageDTOs);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<MessageDTO> getMessage(@PathVariable Long id) {
        return messageRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<MessageDTO>> getMessagesByListing(@PathVariable Long listingId) {
        List<Message> messages = messageRepository.findByListingIdOrderByCreatedAtAsc(listingId);
        List<MessageDTO> messageDTOs = messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messageDTOs);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createMessage(@RequestBody Message message) {
        if (message.getSender() == null || message.getSender().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender is required");
        }
        if (message.getListing() == null || message.getListing().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Listing is required");
        }
        if (!userRepository.existsById(message.getSender().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender not found");
        }
        if (!listingRepository.existsById(message.getListing().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Listing not found");
        }
        
        // Загружаем полные объекты для сохранения
        message.setSender(userRepository.findById(message.getSender().getId()).orElse(null));
        message.setListing(listingRepository.findById(message.getListing().getId()).orElse(null));
        
        Message savedMessage = messageRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedMessage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable Long id, @RequestBody Message messageDetails) {
        return messageRepository.findById(id)
                .map(message -> {
                    message.setContent(messageDetails.getContent());
                    Message updatedMessage = messageRepository.save(message);
                    return ResponseEntity.ok(convertToDTO(updatedMessage));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        if (messageRepository.existsById(id)) {
            messageRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setListingId(message.getListing().getId());
        dto.setListingTitle(message.getListing().getTitle());
        return dto;
    }
}
