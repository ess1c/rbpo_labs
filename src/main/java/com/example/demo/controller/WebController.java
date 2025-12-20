package com.example.demo.controller;

import com.example.demo.entity.Listing;
import com.example.demo.entity.Category;
import com.example.demo.entity.User;
import com.example.demo.service.ListingService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private ListingService listingService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        List<Listing> listings = listingService.getAllActiveListings();
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("listings", listings);
        model.addAttribute("categories", categories);
        return "index";
    }

    @GetMapping("/listings")
    public String getAllListings(Model model) {
        List<Listing> listings = listingService.getAllActiveListings();
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("listings", listings);
        model.addAttribute("categories", categories);
        return "listings";
    }

    @GetMapping("/listings/{id}")
    public String getListingById(@PathVariable Long id, Model model) {
        Listing listing = listingService.getListingById(id);
        model.addAttribute("listing", listing);
        return "listing";
    }

    @GetMapping("/listings/category/{categoryId}")
    public String getListingsByCategory(@PathVariable Long categoryId, Model model) {
        List<Listing> listings = listingService.getListingsByCategory(categoryId);
        Category category = categoryService.getCategoryById(categoryId);
        model.addAttribute("listings", listings);
        model.addAttribute("category", category);
        return "listings";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String email,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setRole("USER");
            
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success", "Регистрация успешна! Теперь вы можете войти.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка регистрации: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/listings/create")
    public String createListingPage(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "create-listing";
    }

    @GetMapping("/listings/{id}/edit")
    public String editListingPage(@PathVariable Long id, Model model) {
        Listing listing = listingService.getListingById(id);
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("listing", listing);
        model.addAttribute("categories", categories);
        return "edit-listing";
    }

    @GetMapping("/my-listings")
    public String myListingsPage() {
        return "my-listings";
    }

    @GetMapping("/messages")
    public String messagesPage() {
        return "messages";
    }
}
