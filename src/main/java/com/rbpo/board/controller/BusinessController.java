package com.rbpo.board.controller;

import com.rbpo.board.model.Listing;
import com.rbpo.board.repository.ListingRepository;
import com.rbpo.board.repository.MessageRepository;
import com.rbpo.board.repository.ReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final ListingRepository listingRepository;
    private final MessageRepository messageRepository;
    private final ReportRepository reportRepository;

    public BusinessController(ListingRepository listingRepository, MessageRepository messageRepository, ReportRepository reportRepository) {
        this.listingRepository = listingRepository;
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/listings/category/{categoryId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Listing>> getListingsByCategory(@PathVariable Long categoryId) {
        List<Listing> listings = listingRepository.findByCategoryId(categoryId);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/listings/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Listing>> getUserListings(@PathVariable Long userId) {
        List<Listing> listings = listingRepository.findByUserId(userId);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/listings/price-range")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Listing>> getListingsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        List<Listing> allListings = listingRepository.findAll();
        List<Listing> filteredListings = allListings.stream()
                .filter(listing -> listing.getPrice().compareTo(minPrice) >= 0 && listing.getPrice().compareTo(maxPrice) <= 0)
                .toList();
        return ResponseEntity.ok(filteredListings);
    }

    @GetMapping("/listing/{listingId}/statistics")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getListingStatistics(@PathVariable Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            return ResponseEntity.notFound().build();
        }
        
        long messageCount = messageRepository.findByListingId(listingId).size();
        long reportCount = reportRepository.findByListingId(listingId).size();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("listingId", listingId);
        statistics.put("messageCount", messageCount);
        statistics.put("reportCount", reportCount);
        
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/listing/{listingId}/conversation")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getListingConversation(@PathVariable Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            return ResponseEntity.notFound().build();
        }
        
        return listingRepository.findById(listingId)
                .map(listing -> {
                    Map<String, Object> conversation = new HashMap<>();
                    conversation.put("listing", listing);
                    conversation.put("messages", messageRepository.findByListingIdOrderByCreatedAtAsc(listingId));
                    return ResponseEntity.ok(conversation);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

