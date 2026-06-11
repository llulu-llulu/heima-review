package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class MerchantOperationAdviceRequest {
    private Long shopId;
    private String businessGoal;
    private String budgetLevel;
}
