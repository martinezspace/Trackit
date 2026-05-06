package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Returns both system categories (userId IS NULL) and user's own categories
    // Ordered by sort_order so UI displays them consistently
    List<Category> findByUserIdIsNullOrUserIdOrderBySortOrderAsc(UUID userId);

    // Used at startup to verify system categories have been seeded
    List<Category> findBySystemTrue();
}