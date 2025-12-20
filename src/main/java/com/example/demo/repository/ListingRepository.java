package com.example.demo.repository;

import com.example.demo.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByCategoryId(Long categoryId);
    List<Listing> findByUserId(Long userId);
    List<Listing> findByIsActiveTrue();
    List<Listing> findByCategoryIdAndIsActiveTrue(Long categoryId);
    
    @Query("SELECT l FROM Listing l WHERE l.isActive = true ORDER BY l.createdAt DESC")
    List<Listing> findAllActiveOrderByCreatedAtDesc();
}

