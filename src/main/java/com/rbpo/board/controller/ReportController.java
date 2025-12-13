package com.rbpo.board.controller;

import com.rbpo.board.dto.ReportDTO;
import com.rbpo.board.model.Report;
import com.rbpo.board.repository.ReportRepository;
import com.rbpo.board.repository.UserRepository;
import com.rbpo.board.repository.ListingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public ReportController(ReportRepository reportRepository, UserRepository userRepository, ListingRepository listingRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        List<Report> reports = reportRepository.findAll();
        List<ReportDTO> reportDTOs = reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reportDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDTO> getReport(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createReport(@RequestBody Report report) {
        if (report.getReporter() == null || report.getReporter().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reporter is required");
        }
        if (report.getListing() == null || report.getListing().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Listing is required");
        }
        if (!userRepository.existsById(report.getReporter().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reporter not found");
        }
        if (!listingRepository.existsById(report.getListing().getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Listing not found");
        }
        if (reportRepository.existsByReporterIdAndListingId(report.getReporter().getId(), report.getListing().getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Report already exists for this listing by this user");
        }
        
        // Загружаем полные объекты для сохранения
        report.setReporter(userRepository.findById(report.getReporter().getId()).orElse(null));
        report.setListing(listingRepository.findById(report.getListing().getId()).orElse(null));
        
        Report savedReport = reportRepository.save(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedReport));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody Report reportDetails) {
        return reportRepository.findById(id)
                .map(report -> {
                    report.setReason(reportDetails.getReason());
                    Report updatedReport = reportRepository.save(report);
                    return ResponseEntity.ok(convertToDTO(updatedReport));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        if (reportRepository.existsById(id)) {
            reportRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setReason(report.getReason());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setListingId(report.getListing().getId());
        dto.setListingTitle(report.getListing().getTitle());
        return dto;
    }
}
