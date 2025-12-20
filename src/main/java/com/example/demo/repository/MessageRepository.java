package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByListingId(Long listingId);
    List<Message> findBySenderId(Long senderId);
    List<Message> findByReceiverId(Long receiverId);
    
    @Query("SELECT m FROM Message m WHERE m.listing.id = :listingId ORDER BY m.createdAt ASC")
    List<Message> findByListingIdOrderByCreatedAtAsc(@Param("listingId") Long listingId);
    
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId OR m.receiver.id = :userId) AND m.listing.id = :listingId ORDER BY m.createdAt ASC")
    List<Message> findConversationByListingAndUser(@Param("listingId") Long listingId, @Param("userId") Long userId);
}

