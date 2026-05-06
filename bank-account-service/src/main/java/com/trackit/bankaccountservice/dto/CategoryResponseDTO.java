package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryResponseDTO {

    private String id;
    private String userId;      // null for system categories
    private String name;
    private String icon;
    private String color;
    private String type;
    private String parentId;   // null for top-level categories
    private boolean system;
    private int sortOrder;
    private String createdAt;
    private List<CategoryResponseDTO> children; // nested subcategories, empty list if none
}