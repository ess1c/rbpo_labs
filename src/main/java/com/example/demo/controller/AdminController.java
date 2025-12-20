package com.example.demo.controller;

import com.example.demo.service.ListingService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ListingService listingService;
    private final CategoryService categoryService;
    private final ReportService reportService;

    public AdminController(ListingService listingService,
                          CategoryService categoryService,
                          ReportService reportService) {
        this.listingService = listingService;
        this.categoryService = categoryService;
        this.reportService = reportService;
    }

    @GetMapping("/listings")
    public String adminListings(Model model) {
        model.addAttribute("listings", listingService.getAllListings());
        return "admin/listings";
    }

    @GetMapping("/categories")
    public String adminCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @GetMapping("/reports")
    public String adminReports(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        return "admin/reports";
    }
}
