package com.rbpo.board.controller;

import com.rbpo.board.dto.ListingDTO;
import com.rbpo.board.model.Listing;
import com.rbpo.board.repository.ListingRepository;
import com.rbpo.board.repository.UserRepository;
import com.rbpo.board.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ListingController(ListingRepository listingRepository, UserRepository userRepository, CategoryRepository categoryRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<ListingDTO>> getAllListings() {
        List<Listing> listings = listingRepository.findAll();
        List<ListingDTO> listingDTOs = listings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(listingDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getListing(@PathVariable Long id) {
        return listingRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createListing(@RequestBody Listing listing) {
        if (!userRepository.existsById(listing.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
        }
        if (!categoryRepository.existsById(listing.getCategory().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category not found");
        }
        Listing savedListing = listingRepository.save(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedListing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(@PathVariable Long id, @RequestBody Listing listingDetails) {
        return listingRepository.findById(id)
                .map(listing -> {
                    listing.setTitle(listingDetails.getTitle());
                    listing.setDescription(listingDetails.getDescription());
                    listing.setPrice(listingDetails.getPrice());
                    if (listingDetails.getCategory() != null && categoryRepository.existsById(listingDetails.getCategory().getId())) {
                        listing.setCategory(listingDetails.getCategory());
                    }
                    Listing updatedListing = listingRepository.save(listing);
                    return ResponseEntity.ok(convertToDTO(updatedListing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        if (listingRepository.existsById(id)) {
            listingRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private ListingDTO convertToDTO(Listing listing) {
        ListingDTO dto = new ListingDTO();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setDescription(listing.getDescription());
        dto.setPrice(listing.getPrice());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setUpdatedAt(listing.getUpdatedAt());
        dto.setUserId(listing.getUser().getId());
        dto.setUsername(listing.getUser().getUsername());
        dto.setCategoryId(listing.getCategory().getId());
        dto.setCategoryName(listing.getCategory().getName());
        return dto;
    }
}
