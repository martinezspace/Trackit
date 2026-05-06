package com.trackit.bankaccountservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Null for system categories shared across all users
    // Set for user-created custom categories
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Icon identifier resolved to actual icon on the frontend e.g. "utensils", "car"
    @Column(name = "icon", length = 50)
    private String icon;

    // Hex color code e.g. "#FF6B6B" - used for category badges in the UI
    @Column(name = "color", length = 7)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    // Optional parent for subcategory grouping e.g. Food > Restaurants
    // Null means this is a top-level category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    // System categories are seeded at startup and cannot be modified or deleted by users
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    // Controls display order in the UI - lower number appears first
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}