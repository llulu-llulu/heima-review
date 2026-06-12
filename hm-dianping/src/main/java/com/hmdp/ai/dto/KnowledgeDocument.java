package com.hmdp.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeDocument {
    private String docId;
    private String docType;
    private Long sourceId;
    private String title;
    private String content;
    private List<String> tags;
    private Double score;
}
