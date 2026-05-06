package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoryCreateDTO {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be 100 characters or less")
    private String name;

    // Nullable - icon identifier resolved on frontend e.g. "utensils", "car"
    @Size(max = 50, message = "Icon must be 50 characters or less")
    private String icon;

    // Nullable - hex color for UI badge e.g. "#FF6B6B"
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code e.g. #FF6B6B")
    private String color;

    @NotNull(message = "Category type is required")
    private CategoryType type;

    // Nullable - omit for top-level categories
    private UUID parentId;
}