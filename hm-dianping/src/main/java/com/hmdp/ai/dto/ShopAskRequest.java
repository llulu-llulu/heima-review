package com.hmdp.ai.dto;

import lombok.Data;

@Data
public class ShopAskRequest {
    private String question;
    private Long userId;
    private Double x;
    private Double y;
    private Long typeId;
    private Integer limit;
}
