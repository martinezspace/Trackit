package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.Category;
import com.trackit.bankaccountservice.model.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID userId;
    private UUID otherUserId;
    private Category systemFood;
    private Category systemTransport;
    private Category userCustom;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        // System categories - userId is null, shared across all users
        systemFood = new Category();
        systemFood.setName("Food & Dining");
        systemFood.setType(CategoryType.DEBIT);
        systemFood.setSystem(true);
        systemFood.setSortOrder(1);
        categoryRepository.save(systemFood);

        systemTransport = new Category();
        systemTransport.setName("Transport");
        systemTransport.setType(CategoryType.DEBIT);
        systemTransport.setSystem(true);
        systemTransport.setSortOrder(2);
        categoryRepository.save(systemTransport);

        userCustom = new Category();
        userCustom.setUserId(userId);
        userCustom.setName("My Savings Goal");
        userCustom.setType(CategoryType.BOTH);
        userCustom.setSystem(false);
        userCustom.setSortOrder(10);
        categoryRepository.save(userCustom);

        // Category for otherUserId - should not appear in userId queries
        Category otherUserCategory = new Category();
        otherUserCategory.setUserId(otherUserId);
        otherUserCategory.setName("Other User Category");
        otherUserCategory.setType(CategoryType.DEBIT);
        otherUserCategory.setSystem(false);
        otherUserCategory.setSortOrder(10);
        categoryRepository.save(otherUserCategory);
    }

    // findByUserIdIsNullOrUserId

    @Test
    public void findByUserIdIsNullOrUserId_returnsSystemAndOwnCategories() {
        List<Category> result = categoryRepository
                .findByUserIdIsNullOrUserIdOrderBySortOrderAsc(userId);

        // 2 system + 1 own = 3, other user's category excluded
        assertThat(result).hasSize(3);
        assertThat(result).anyMatch(c -> c.getName().equals("Food & Dining") && c.isSystem());
        assertThat(result).anyMatch(c -> c.getName().equals("Transport") && c.isSystem());
        assertThat(result).anyMatch(c -> c.getName().equals("My Savings Goal") && !c.isSystem());
    }

    @Test
    public void findByUserIdIsNullOrUserId_doesNotReturnOtherUsersCategories() {
        List<Category> result = categoryRepository
                .findByUserIdIsNullOrUserIdOrderBySortOrderAsc(userId);

        assertThat(result).noneMatch(c -> c.getName().equals("Other User Category"));
    }

    @Test
    public void findByUserIdIsNullOrUserId_orderedBySortOrder() {
        List<Category> result = categoryRepository
                .findByUserIdIsNullOrUserIdOrderBySortOrderAsc(userId);

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getSortOrder())
                    .isLessThanOrEqualTo(result.get(i + 1).getSortOrder());
        }
    }

    // findBySystemTrue

    @Test
    public void findBySystemTrue_returnsOnlySystemCategories() {
        List<Category> result = categoryRepository.findBySystemTrue();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Category::isSystem);
        assertThat(result).noneMatch(c -> c.getUserId() != null);
    }

    // parent-child

    @Test
    public void save_persistsParentChildRelationship() {
        Category child = new Category();
        child.setUserId(userId);
        child.setName("Restaurants");
        child.setType(CategoryType.DEBIT);
        child.setParent(systemFood);
        child.setSystem(false);
        child.setSortOrder(1);
        categoryRepository.save(child);

        Category found = categoryRepository.findById(child.getId()).orElseThrow();
        assertThat(found.getParent()).isNotNull();
        assertThat(found.getParent().getId()).isEqualTo(systemFood.getId());
    }

    // delete

    @Test
    public void delete_removesUserCategory() {
        categoryRepository.delete(userCustom);

        assertThat(categoryRepository.findById(userCustom.getId())).isEmpty();
    }
}