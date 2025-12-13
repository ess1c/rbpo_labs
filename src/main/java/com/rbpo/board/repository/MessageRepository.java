package com.rbpo.board.repository;

import com.rbpo.board.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByListingId(Long listingId);
    List<Message> findBySenderId(Long senderId);
    List<Message> findByListingIdOrderByCreatedAtAsc(Long listingId);
}

