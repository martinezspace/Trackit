package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.CategoryCreateDTO;
import com.trackit.bankaccountservice.dto.CategoryResponseDTO;
import com.trackit.bankaccountservice.dto.CategoryUpdateDTO;
import com.trackit.bankaccountservice.model.Category;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class CategoryMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Entity => ResponseDTO
    // children passed in separately - avoids lazy loading issues when called outside a transaction
    public CategoryResponseDTO toResponseDTO(Category category, List<CategoryResponseDTO> children) {
        CategoryResponseDTO response = new CategoryResponseDTO();
        response.setId(category.getId().toString());
        response.setUserId(category.getUserId() != null
                ? category.getUserId().toString() : null);
        response.setName(category.getName());
        response.setIcon(category.getIcon());
        response.setColor(category.getColor());
        response.setType(category.getType().name());
        response.setParentId(category.getParent() != null
                ? category.getParent().getId().toString() : null);
        response.setSystem(category.isSystem());
        response.setSortOrder(category.getSortOrder());
        response.setCreatedAt(category.getCreatedAt() != null
                ? category.getCreatedAt().format(DATE_FORMAT) : null);
        response.setChildren(children);
        return response;
    }

    // CreateDTO => Entity
    // parent resolved in service layer before calling this mapper
    public Category toEntity(CategoryCreateDTO request, Category parent, UUID userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setType(request.getType());
        category.setParent(parent);
        category.setSystem(false);
        return category;
    }

    // UpdateDTO => Entity - applies only non-null fields onto existing entity
    public Category applyUpdate(Category existing, CategoryUpdateDTO request) {
        if (request.getName() != null) existing.setName(request.getName());
        if (request.getIcon() != null) existing.setIcon(request.getIcon());
        if (request.getColor() != null) existing.setColor(request.getColor());
        if (request.getType() != null) existing.setType(request.getType());
        return existing;
    }
}