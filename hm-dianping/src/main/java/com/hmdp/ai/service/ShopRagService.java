package com.hmdp.ai.service;

import com.hmdp.ai.dto.KnowledgeDocument;
import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import com.hmdp.ai.dto.ShopAskRequest;
import com.hmdp.ai.dto.ShopAskResponse;
import com.hmdp.ai.prompt.PromptTemplateService;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopRagService {

    private static final String NO_EVIDENCE_ANSWER = "当前数据不足，无法基于已有店铺信息回答";

    private final KnowledgeIndexService knowledgeIndexService;
    private final IShopService shopService;
    private final LlmService llmService;
    private final PromptTemplateService promptTemplateService;

    public ShopRagService(KnowledgeIndexService knowledgeIndexService,
                          IShopService shopService,
                          LlmService llmService,
                          PromptTemplateService promptTemplateService) {
        this.knowledgeIndexService = knowledgeIndexService;
        this.shopService = shopService;
        this.llmService = llmService;
        this.promptTemplateService = promptTemplateService;
    }

    public ShopAskResponse ask(ShopAskRequest request) {
        int limit = request.getLimit() == null ? 5 : Math.max(1, Math.min(request.getLimit(), 20));
        List<KnowledgeDocument> documents = knowledgeIndexService.search(request.getQuestion(), request.getTypeId(), limit);
        if (documents.isEmpty()) {
            ShopAskResponse response = new ShopAskResponse();
            response.setAnswer(NO_EVIDENCE_ANSWER);
            response.setRecommendedShops(Collections.emptyList());
            response.setReferences(Collections.emptyList());
            response.setConfidence(0.0);
            response.setNeedMoreInfo(true);
            return response;
        }

        LlmRequest llmRequest = new LlmRequest();
        llmRequest.setScene("shop-rag-qa");
        llmRequest.setSystemPrompt(promptTemplateService.load("shop-rag-qa"));
        llmRequest.setUserPrompt("问题：" + request.getQuestion() + "\n证据：\n" + buildContext(documents));
        LlmResponse llmResponse = llmService.chat(llmRequest);

        List<Long> shopIds = documents.stream()
                .filter(document -> "shop".equals(document.getDocType()))
                .map(KnowledgeDocument::getSourceId)
                .distinct()
                .collect(Collectors.toList());
        List<Shop> shops = shopIds.isEmpty() ? Collections.emptyList() : shopService.listByIds(shopIds);

        ShopAskResponse response = new ShopAskResponse();
        response.setAnswer(llmResponse.getContent());
        response.setRecommendedShops(shops);
        response.setReferences(documents.stream().map(KnowledgeDocument::getDocId).collect(Collectors.toList()));
        response.setConfidence(Math.min(0.95, 0.5 + documents.size() * 0.08));
        response.setNeedMoreInfo(false);
        return response;
    }

    public int reindex() {
        return knowledgeIndexService.rebuild().size();
    }

    public boolean invalidQuestion(String question) {
        return !StringUtils.hasText(question);
    }

    private String buildContext(List<KnowledgeDocument> documents) {
        return documents.stream()
                .map(document -> document.getDocId() + " " + document.getTitle() + " " + document.getContent())
                .collect(Collectors.joining("\n"));
    }
}
