package com.rbpo.board.repository;

import com.rbpo.board.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByUserId(Long userId);
    List<Listing> findByCategoryId(Long categoryId);
    List<Listing> findByUserIdAndCategoryId(Long userId, Long categoryId);
}

