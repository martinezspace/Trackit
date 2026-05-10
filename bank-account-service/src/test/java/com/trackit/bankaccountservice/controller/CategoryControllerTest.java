package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.dto.CategoryCreateDTO;
import com.trackit.bankaccountservice.dto.CategoryResponseDTO;
import com.trackit.bankaccountservice.dto.CategoryUpdateDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.CategoryType;
import com.trackit.bankaccountservice.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private UUID userId;
    private UUID categoryId;
    private CategoryResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        testResponseDTO = new CategoryResponseDTO();
        testResponseDTO.setId(categoryId.toString());
        testResponseDTO.setName("Food & Dining");
        testResponseDTO.setType("DEBIT");
        testResponseDTO.setSystem(true);
    }

    // GET /api/categories

    @Test
    public void getAllCategories_returns200_withNestedTree() throws Exception {
        when(categoryService.getAllCategoriesForUser(userId)).thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/categories")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Food & Dining"))
                .andExpect(jsonPath("$[0].system").value(true));
    }

    @Test
    public void getAllCategories_returns200_withEmptyList() throws Exception {
        when(categoryService.getAllCategoriesForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/categories")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/categories/{id}

    @Test
    public void getCategory_returns200_whenFound() throws Exception {
        when(categoryService.getCategoryById(categoryId)).thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food & Dining"));
    }

    @Test
    public void getCategory_returns404_whenNotFound() throws Exception {
        when(categoryService.getCategoryById(categoryId))
                .thenThrow(new ResourceNotFoundException("Category not found: " + categoryId));

        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound());
    }

    // POST /api/categories

    @Test
    public void createCategory_returns201_withCreatedDTO() throws Exception {
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("My Savings");
        createDTO.setType(CategoryType.BOTH);

        CategoryResponseDTO createdResponse = new CategoryResponseDTO();
        createdResponse.setName("My Savings");
        createdResponse.setSystem(false);

        when(categoryService.createCategory(eq(userId), any(CategoryCreateDTO.class)))
                .thenReturn(createdResponse);

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Savings"));
    }

    @Test
    public void createCategory_returns422_whenNameMissing() throws Exception {
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setType(CategoryType.DEBIT);
        // name intentionally missing

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    public void createCategory_returns422_whenTypeMissing() throws Exception {
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("My Savings");
        // type intentionally missing

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    // PATCH /api/categories/{id}

    @Test
    public void updateCategory_returns200_whenValid() throws Exception {
        CategoryUpdateDTO updateDTO = new CategoryUpdateDTO();
        updateDTO.setName("Updated Name");

        CategoryResponseDTO updatedResponse = new CategoryResponseDTO();
        updatedResponse.setName("Updated Name");

        when(categoryService.updateCategory(eq(categoryId), eq(userId), any(CategoryUpdateDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/categories/{id}", categoryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    public void updateCategory_returns400_whenSystemCategory() throws Exception {
        doThrow(new IllegalArgumentException("System categories cannot be modified"))
                .when(categoryService).updateCategory(any(), any(), any());

        mockMvc.perform(patch("/api/categories/{id}", categoryId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // DELETE /api/categories/{id}

    @Test
    public void deleteCategory_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(categoryId, userId);
    }

    @Test
    public void deleteCategory_returns400_whenSystemCategory() throws Exception {
        doThrow(new IllegalArgumentException("System categories cannot be deleted"))
                .when(categoryService).deleteCategory(any(), any());

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}