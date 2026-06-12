package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class MerchantReviewSummaryRequest {
    private Long shopId;
    private Integer maxReviews;
}
