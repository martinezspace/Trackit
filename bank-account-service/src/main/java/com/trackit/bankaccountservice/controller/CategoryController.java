package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.CategoryCreateDTO;
import com.trackit.bankaccountservice.dto.CategoryResponseDTO;
import com.trackit.bankaccountservice.dto.CategoryUpdateDTO;
import com.trackit.bankaccountservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "API for managing transaction categories")
public class CategoryController {

    private final CategoryService service;

    //GET /api/categories
    @GetMapping
    @Operation(summary = "Get all categories for authenticated user, includes system categories and user's own, returned as nested tree")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAllCategoriesForUser(userId));
    }

    //GET /api/categories/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get single category by id, no ownership check as categories can be read freely")
    public ResponseEntity<CategoryResponseDTO> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategoryById(id));
    }

    //POST /api/categories
    @PostMapping
    @Operation(summary = "Create new user category")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CategoryCreateDTO request) {
        CategoryResponseDTO response = service.createCategory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //PATCH /api/categories/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Update user category, returns 400 if attempting to modify a system category")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody CategoryUpdateDTO request) {
        return ResponseEntity.ok(service.updateCategory(id, userId, request));
    }

    //DELETE /api/categories/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user category, returns 400 if attempting to delete a system category")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        service.deleteCategory(id, userId);
        return ResponseEntity.noContent().build();
    }
}