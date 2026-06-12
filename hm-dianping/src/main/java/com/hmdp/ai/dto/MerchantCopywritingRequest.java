package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class MerchantCopywritingRequest {
    private Long shopId;
    private String activityType;
    private String targetUser;
    private String tone;
    private String extraRequirement;
}
