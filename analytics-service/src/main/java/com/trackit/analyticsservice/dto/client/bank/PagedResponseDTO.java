package com.trackit.analyticsservice.dto.client.bank;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Mirrors bank-account-service PagedResponseDTO for deserializing paginated transaction responses
@Getter
@Setter
public class PagedResponseDTO<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}