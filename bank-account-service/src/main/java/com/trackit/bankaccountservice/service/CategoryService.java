package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.CategoryCreateDTO;
import com.trackit.bankaccountservice.dto.CategoryResponseDTO;
import com.trackit.bankaccountservice.dto.CategoryUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.CategoryMapper;
import com.trackit.bankaccountservice.model.Category;
import com.trackit.bankaccountservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    // Queries

    public List<CategoryResponseDTO> getAllCategoriesForUser(UUID userId) {
        List<Category> all = repository.findByUserIdIsNullOrUserIdOrderBySortOrderAsc(userId);

        // Build nested tree - top-level categories contain their children inline
        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(parent -> {
                    List<CategoryResponseDTO> children = all.stream()
                            .filter(c -> c.getParent() != null
                                    && c.getParent().getId().equals(parent.getId()))
                            .map(child -> mapper.toResponseDTO(child, List.of()))
                            .toList();
                    return mapper.toResponseDTO(parent, children);
                })
                .toList();
    }

    public CategoryResponseDTO getCategoryById(UUID id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return mapper.toResponseDTO(category, List.of());
    }

    // Commands

    public CategoryResponseDTO createCategory(UUID userId, CategoryCreateDTO request) {
        Category parent = request.getParentId() != null
                ? repository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent category not found: " + request.getParentId()))
                : null;
        Category category = mapper.toEntity(request, parent, userId);
        return mapper.toResponseDTO(repository.save(category), List.of());
    }

    public CategoryResponseDTO updateCategory(UUID id, UUID userId, CategoryUpdateDTO request) {
        Category existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        // System categories are shared across all users and cannot be modified
        if (existing.isSystem()) {
            throw new IllegalArgumentException("System categories cannot be modified");
        }
        // Users can only modify their own categories
        if (!userId.equals(existing.getUserId())) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }

        Category updated = mapper.applyUpdate(existing, request);
        return mapper.toResponseDTO(repository.save(updated), List.of());
    }

    public void deleteCategory(UUID id, UUID userId) {
        Category existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        if (existing.isSystem()) {
            throw new IllegalArgumentException("System categories cannot be deleted");
        }
        if (!userId.equals(existing.getUserId())) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }

        repository.delete(existing);
    }
}