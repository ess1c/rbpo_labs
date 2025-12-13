package com.rbpo.board.repository;

import com.rbpo.board.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByListingId(Long listingId);
    List<Report> findByReporterId(Long reporterId);
    Optional<Report> findByReporterIdAndListingId(Long reporterId, Long listingId);
    boolean existsByReporterIdAndListingId(Long reporterId, Long listingId);
}

