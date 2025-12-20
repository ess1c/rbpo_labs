package com.example.demo.service;

import com.example.demo.entity.Listing;
import com.example.demo.entity.Report;
import com.example.demo.entity.User;
import com.example.demo.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ListingService listingService;
    private final UserService userService;

    public ReportService(ReportRepository reportRepository,
                        ListingService listingService,
                        UserService userService) {
        this.reportRepository = reportRepository;
        this.listingService = listingService;
        this.userService = userService;
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public List<Report> getReportsByListing(Long listingId) {
        return reportRepository.findByListingId(listingId);
    }

    public List<Report> getReportsByUser(Long userId) {
        return reportRepository.findByUserId(userId);
    }

    public List<Report> getReportsByStatus(String status) {
        return reportRepository.findByStatus(status);
    }

    public Report getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + id));
    }

    @Transactional
    public Report createReport(Report report, Long listingId, Long userId) {
        Listing listing = listingService.getListingById(listingId);
        User user = userService.getUserById(userId);
        
        if (listing.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Cannot report your own listing");
        }
        
        if (reportRepository.findByUserIdAndListingId(userId, listingId).isPresent()) {
            throw new IllegalStateException("You have already reported this listing");
        }
        
        report.setListing(listing);
        report.setUser(user);
        report.setStatus("PENDING");
        
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateReportStatus(Long id, String status) {
        Report report = getReportById(id);
        
        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid status. Must be PENDING, APPROVED, or REJECTED");
        }
        
        report.setStatus(status);
        return reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new IllegalArgumentException("Report not found with id: " + id);
        }
        reportRepository.deleteById(id);
    }
}

