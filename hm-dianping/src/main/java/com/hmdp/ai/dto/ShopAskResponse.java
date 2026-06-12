package com.hmdp.ai.dto;

import com.hmdp.entity.Shop;
import lombok.Data;

import java.util.List;

@Data
public class ShopAskResponse {
    private String answer;
    private List<Shop> recommendedShops;
    private List<String> references;
    private Double confidence;
    private Boolean needMoreInfo;
}
