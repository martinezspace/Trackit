package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.CategoryCreateDTO;
import com.trackit.bankaccountservice.dto.CategoryResponseDTO;
import com.trackit.bankaccountservice.dto.CategoryUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.CategoryMapper;
import com.trackit.bankaccountservice.model.Category;
import com.trackit.bankaccountservice.model.CategoryType;
import com.trackit.bankaccountservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private UUID userId;
    private UUID categoryId;
    private Category systemCategory;
    private Category userCategory;
    private CategoryResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        // System category - cannot be modified or deleted by users
        systemCategory = new Category();
        systemCategory.setName("Food & Dining");
        systemCategory.setType(CategoryType.DEBIT);
        systemCategory.setSystem(true);
        systemCategory.setSortOrder(1);

        // User-created category - can be modified and deleted
        userCategory = new Category();
        userCategory.setUserId(userId);
        userCategory.setName("My Savings");
        userCategory.setType(CategoryType.BOTH);
        userCategory.setSystem(false);
        userCategory.setSortOrder(10);

        testResponseDTO = new CategoryResponseDTO();
        testResponseDTO.setId(categoryId.toString());
        testResponseDTO.setName("My Savings");
        testResponseDTO.setSystem(false);
    }

    // getCategoryById

    @Test
    public void getCategoryById_returnsDTO_whenCategoryExists() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));
        when(categoryMapper.toResponseDTO(eq(userCategory), any())).thenReturn(testResponseDTO);

        CategoryResponseDTO result = categoryService.getCategoryById(categoryId);

        assertThat(result.getName()).isEqualTo("My Savings");
    }

    @Test
    public void getCategoryById_throwsException_whenCategoryNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    // createCategory

    @Test
    public void createCategory_savesAndReturnsDTO() {
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("My Savings");
        createDTO.setType(CategoryType.BOTH);

        when(categoryMapper.toEntity(createDTO, null, userId)).thenReturn(userCategory);
        when(categoryRepository.save(userCategory)).thenReturn(userCategory);
        when(categoryMapper.toResponseDTO(eq(userCategory), any())).thenReturn(testResponseDTO);

        CategoryResponseDTO result = categoryService.createCategory(userId, createDTO);

        assertThat(result.getName()).isEqualTo("My Savings");
        verify(categoryRepository, times(1)).save(userCategory);
    }

    @Test
    public void createCategory_resolvesParent_whenParentIdProvided() {
        UUID parentId = UUID.randomUUID();
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("Restaurants");
        createDTO.setType(CategoryType.DEBIT);
        createDTO.setParentId(parentId);

        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(systemCategory));
        when(categoryMapper.toEntity(createDTO, systemCategory, userId)).thenReturn(userCategory);
        when(categoryRepository.save(userCategory)).thenReturn(userCategory);
        when(categoryMapper.toResponseDTO(eq(userCategory), any())).thenReturn(testResponseDTO);

        categoryService.createCategory(userId, createDTO);

        verify(categoryRepository, times(1)).findById(parentId);
        verify(categoryMapper, times(1)).toEntity(createDTO, systemCategory, userId);
    }

    @Test
    public void createCategory_throwsException_whenParentCategoryNotFound() {
        UUID parentId = UUID.randomUUID();
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("Restaurants");
        createDTO.setType(CategoryType.DEBIT);
        createDTO.setParentId(parentId);

        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(userId, createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent category not found");

        verify(categoryRepository, never()).save(any());
    }

    // updateCategory

    @Test
    public void updateCategory_appliesChangesAndReturnsDTO() {
        CategoryUpdateDTO updateDTO = new CategoryUpdateDTO();
        updateDTO.setName("Updated Name");

        CategoryResponseDTO updatedResponse = new CategoryResponseDTO();
        updatedResponse.setName("Updated Name");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));
        when(categoryMapper.applyUpdate(userCategory, updateDTO)).thenReturn(userCategory);
        when(categoryRepository.save(userCategory)).thenReturn(userCategory);
        when(categoryMapper.toResponseDTO(eq(userCategory), any())).thenReturn(updatedResponse);

        CategoryResponseDTO result = categoryService.updateCategory(categoryId, userId, updateDTO);

        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(categoryMapper, times(1)).applyUpdate(userCategory, updateDTO);
        verify(categoryRepository, times(1)).save(userCategory);
    }

    @Test
    public void updateCategory_throwsException_whenSystemCategory() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, userId, new CategoryUpdateDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("System categories cannot be modified");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void updateCategory_throwsException_whenCategoryBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, differentUserId, new CategoryUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    // deleteCategory

    @Test
    public void deleteCategory_deletesWhenOwnerAndNotSystem() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));

        categoryService.deleteCategory(categoryId, userId);

        verify(categoryRepository, times(1)).delete(userCategory);
    }

    @Test
    public void deleteCategory_throwsException_whenSystemCategory() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(systemCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("System categories cannot be deleted");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    public void deleteCategory_throwsException_whenCategoryBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}