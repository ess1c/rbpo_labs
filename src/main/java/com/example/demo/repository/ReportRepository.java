package com.example.demo.repository;

import com.example.demo.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByListingId(Long listingId);
    List<Report> findByUserId(Long userId);
    List<Report> findByStatus(String status);
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.listing.id = :listingId")
    Optional<Report> findByUserIdAndListingId(@Param("userId") Long userId, @Param("listingId") Long listingId);
}

