package com.example.demo.service;

import com.example.demo.entity.Category;
import com.example.demo.entity.Listing;
import com.example.demo.entity.User;
import com.example.demo.repository.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final CategoryService categoryService;
    private final UserService userService;

    public ListingService(ListingRepository listingRepository,
                         CategoryService categoryService,
                         UserService userService) {
        this.listingRepository = listingRepository;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    public List<Listing> getAllActiveListings() {
        return listingRepository.findAllActiveOrderByCreatedAtDesc();
    }

    public List<Listing> getListingsByCategory(Long categoryId) {
        return listingRepository.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    public List<Listing> getListingsByUser(Long userId) {
        return listingRepository.findByUserId(userId);
    }

    public Listing getListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + id));
    }

    @Transactional
    public Listing createListing(Listing listing, Long userId, Long categoryId) {
        User user = userService.getUserById(userId);
        Category category = categoryService.getCategoryById(categoryId);
        
        listing.setUser(user);
        listing.setCategory(category);
        listing.setIsActive(true);
        
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(Long id, Listing updatedListing, Long userId, String userRole) {
        Listing existing = getListingById(id);
        
        if (!"ADMIN".equals(userRole) && !existing.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only update your own listings");
        }
        
        existing.setTitle(updatedListing.getTitle());
        existing.setDescription(updatedListing.getDescription());
        existing.setPrice(updatedListing.getPrice());
        
        if (updatedListing.getCategory() != null) {
            Category category = categoryService.getCategoryById(updatedListing.getCategory().getId());
            existing.setCategory(category);
        }
        
        return listingRepository.save(existing);
    }

    @Transactional
    public void deleteListing(Long id, Long userId, String userRole) {
        Listing listing = getListingById(id);
        
        if (!"ADMIN".equals(userRole) && !listing.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own listings");
        }
        
        listingRepository.deleteById(id);
    }

    @Transactional
    public Listing deactivateListing(Long id, Long userId, String userRole) {
        Listing listing = getListingById(id);
        
        if (!"ADMIN".equals(userRole) && !listing.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only deactivate your own listings");
        }
        
        listing.setIsActive(false);
        return listingRepository.save(listing);
    }
}

