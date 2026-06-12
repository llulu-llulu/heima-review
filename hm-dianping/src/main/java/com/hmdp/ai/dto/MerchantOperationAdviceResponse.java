package com.hmdp.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantOperationAdviceResponse {
    private List<String> adviceList;
    private String priority;
    private String expectedEffect;
    private List<String> riskTips;
    private List<String> references;
}
