package com.hmdp.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class MerchantCopywritingResponse {
    private String title;
    private String content;
    private List<String> sellingPoints;
    private List<String> riskTips;
    private List<String> references;
}
