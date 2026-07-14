package com.trackit.analyticsservice.mapper;

import com.trackit.analyticsservice.dto.response.AssetAllocationResponseDTO;
import com.trackit.analyticsservice.model.AssetAllocation;
import org.springframework.stereotype.Component;

@Component
public class AssetAllocationMapper {

    public AssetAllocationResponseDTO toResponseDTO(AssetAllocation allocation) {
        AssetAllocationResponseDTO response = new AssetAllocationResponseDTO();
        response.setId(allocation.getId().toString());
        response.setUserId(allocation.getUserId().toString());
        response.setAccountId(allocation.getAccountId().toString());
        response.setAccountDisplayName(allocation.getAccountDisplayName());
        response.setAccountType(allocation.getAccountType());
        response.setInstrumentType(allocation.getInstrumentType());
        response.setCurrentValue(allocation.getCurrentValue() != null
                ? allocation.getCurrentValue().toPlainString() : null);
        response.setWeightPct(allocation.getWeightPct() != null
                ? allocation.getWeightPct().toPlainString() : null);
        response.setCurrency(allocation.getCurrency());
        response.setUpdatedAt(allocation.getUpdatedAt() != null
                ? allocation.getUpdatedAt().toString() : null);
        return response;
    }
}