package com.hmdp.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantReviewSummaryResponse {
    private String positiveSummary;
    private String negativeSummary;
    private List<String> commonKeywords;
    private List<String> improvementSuggestions;
    private List<String> references;
}
