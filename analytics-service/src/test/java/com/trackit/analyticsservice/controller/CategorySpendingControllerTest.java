package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.CategorySpendingSummaryResponseDTO;
import com.trackit.analyticsservice.exception.GlobalExceptionHandler;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.service.CategorySpendingSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategorySpendingController.class)
@Import(GlobalExceptionHandler.class)
class CategorySpendingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategorySpendingSyncService service;

    private UUID userId;
    private UUID categoryId;
    private CategorySpendingSummaryResponseDTO testDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        testDTO = new CategorySpendingSummaryResponseDTO();
        testDTO.setId(UUID.randomUUID().toString());
        testDTO.setUserId(userId.toString());
        testDTO.setCategoryId(categoryId.toString());
        testDTO.setCategoryName("Groceries");
        testDTO.setCategoryColor("#00FF00");
        testDTO.setPeriodYear(2024);
        testDTO.setPeriodMonth(3);
        testDTO.setTotalAmount("300.00");
        testDTO.setTransactionCount(5);
        testDTO.setCurrency("EUR");
    }

    // GET /api/analytics/category-spending/{year}/{month}

    @Test
    void getByPeriod_returns200WithList() throws Exception {
        when(service.getByPeriod(userId, 2024, 3)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/analytics/category-spending/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$[0].totalAmount").value("300.00"));
    }

    @Test
    void getByPeriod_returns200WithEmptyList_whenNoSummaries() throws Exception {
        when(service.getByPeriod(userId, 2024, 3)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/category-spending/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/analytics/category-spending/{year}/{month}/{categoryId}

    @Test
    void getByPeriodAndCategory_returns200WithDTO() throws Exception {
        when(service.getByPeriodAndCategory(userId, 2024, 3, categoryId)).thenReturn(testDTO);

        mockMvc.perform(get("/api/analytics/category-spending/2024/3/" + categoryId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.totalAmount").value("300.00"));
    }

    @Test
    void getByPeriodAndCategory_returns404_whenNotFound() throws Exception {
        when(service.getByPeriodAndCategory(userId, 2024, 3, categoryId))
                .thenThrow(new ResourceNotFoundException("No spending summary found"));

        mockMvc.perform(get("/api/analytics/category-spending/2024/3/" + categoryId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/analytics/category-spending/sync/{year}/{month}

    @Test
    void sync_returns204() throws Exception {
        doNothing().when(service).sync(userId, 2024, 3);

        mockMvc.perform(post("/api/analytics/category-spending/sync/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }
}