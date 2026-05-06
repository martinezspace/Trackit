package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.CategoryType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryUpdateDTO {

    // All fields nullable - only provided fields are applied in the service

    @Size(max = 100, message = "Category name must be 100 characters or less")
    private String name;

    @Size(max = 50, message = "Icon must be 50 characters or less")
    private String icon;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code e.g. #FF6B6B")
    private String color;

    private CategoryType type;
}