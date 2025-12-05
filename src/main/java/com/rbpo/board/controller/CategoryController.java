package com.rbpo.board.controller;

import com.rbpo.board.dto.CategoryDTO;
import com.rbpo.board.model.Category;
import com.rbpo.board.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDTO> categoryDTOs = categories.stream()
                .map(category -> new CategoryDTO(category.getId(), category.getName(), category.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categoryDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> new CategoryDTO(category.getId(), category.getName(), category.getDescription()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Category name already exists");
        }
        Category savedCategory = categoryRepository.save(category);
        CategoryDTO categoryDTO = new CategoryDTO(savedCategory.getId(), savedCategory.getName(), savedCategory.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        return categoryRepository.findById(id)
                .map(category -> {
                    if (!category.getName().equals(categoryDetails.getName()) && categoryRepository.existsByName(categoryDetails.getName())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Category name already exists");
                    }
                    category.setName(categoryDetails.getName());
                    category.setDescription(categoryDetails.getDescription());
                    Category updatedCategory = categoryRepository.save(category);
                    CategoryDTO categoryDTO = new CategoryDTO(updatedCategory.getId(), updatedCategory.getName(), updatedCategory.getDescription());
                    return ResponseEntity.ok(categoryDTO);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
