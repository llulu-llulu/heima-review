package com.hmdp.ai.service;

import com.hmdp.ai.dto.AgentRecommendRequest;
import com.hmdp.ai.dto.AgentRecommendResponse;
import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import com.hmdp.ai.log.AgentToolCallLog;
import com.hmdp.ai.prompt.PromptTemplateService;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationAgentService {

    private final IShopService shopService;
    private final IVoucherService voucherService;
    private final IBlogService blogService;
    private final LlmService llmService;
    private final PromptTemplateService promptTemplateService;

    public RecommendationAgentService(IShopService shopService,
                                      IVoucherService voucherService,
                                      IBlogService blogService,
                                      LlmService llmService,
                                      PromptTemplateService promptTemplateService) {
        this.shopService = shopService;
        this.voucherService = voucherService;
        this.blogService = blogService;
        this.llmService = llmService;
        this.promptTemplateService = promptTemplateService;
    }

    public boolean invalidQuery(String query) {
        return !StringUtils.hasText(query);
    }

    public AgentRecommendResponse recommend(AgentRecommendRequest request) {
        int limit = request.getMaxCandidates() == null ? 5 : Math.max(1, Math.min(request.getMaxCandidates(), 20));
        List<AgentToolCallLog> toolLogs = new ArrayList<>();
        List<Shop> candidates = record(toolLogs, "searchNearbyShops",
                "typeId=" + request.getTypeId() + ",limit=" + limit,
                () -> searchNearbyShops(request.getTypeId(), limit));
        if (candidates.isEmpty()) {
            AgentRecommendResponse response = new AgentRecommendResponse();
            response.setAnswer("没有找到符合条件的候选店铺");
            response.setShops(Collections.emptyList());
            response.setReasons(Collections.emptyList());
            response.setUsedTools(toolNames(toolLogs));
            response.setReferences(Collections.emptyList());
            response.setNeedMoreInfo(true);
            return response;
        }

        List<Shop> ranked = record(toolLogs, "rankCandidateShops", "count=" + candidates.size(),
                () -> rankCandidateShops(candidates));
        List<String> references = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        for (Shop shop : ranked) {
            Shop detail = record(toolLogs, "getShopDetail", "shopId=" + shop.getId(), () -> getShopDetail(shop.getId()));
            List<Voucher> vouchers = record(toolLogs, "getShopVoucherInfo", "shopId=" + shop.getId(), () -> getShopVoucherInfo(shop.getId()));
            List<Blog> blogs = record(toolLogs, "getShopContentEvidence", "shopId=" + shop.getId(), () -> getShopContentEvidence(shop.getId()));
            references.add("shop:" + detail.getId() + ":" + detail.getName());
            vouchers.forEach(voucher -> references.add("voucher:" + voucher.getId() + ":" + voucher.getTitle()));
            blogs.forEach(blog -> references.add("blog:" + blog.getId() + ":" + blog.getTitle()));
            reasons.add(detail.getName() + "：评分、销量、优惠和内容证据综合靠前");
        }

        LlmResponse llmResponse = callLlm(request, ranked, references);
        toolLogs.forEach(this::logTool);

        AgentRecommendResponse response = new AgentRecommendResponse();
        response.setAnswer(llmResponse.getContent());
        response.setShops(ranked);
        response.setReasons(reasons);
        response.setUsedTools(toolNames(toolLogs));
        response.setReferences(references);
        response.setNeedMoreInfo(false);
        return response;
    }

    private List<Shop> searchNearbyShops(Long typeId, int limit) {
        if (typeId == null) {
            return shopService.query().orderByDesc("score").last("LIMIT " + limit).list();
        }
        return shopService.query().eq("type_id", typeId).orderByDesc("score").last("LIMIT " + limit).list();
    }

    private Shop getShopDetail(Long shopId) {
        return shopService.getById(shopId);
    }

    private List<Voucher> getShopVoucherInfo(Long shopId) {
        return voucherService.query().eq("shop_id", shopId).last("LIMIT 3").list();
    }

    private List<Blog> getShopContentEvidence(Long shopId) {
        return blogService.query().eq("shop_id", shopId).orderByDesc("liked").last("LIMIT 3").list();
    }

    private List<Shop> rankCandidateShops(List<Shop> shops) {
        return shops.stream()
                .sorted(Comparator
                        .comparing((Shop shop) -> shop.getScore() == null ? 0 : shop.getScore()).reversed()
                        .thenComparing((Shop shop) -> shop.getSold() == null ? 0 : shop.getSold(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private LlmResponse callLlm(AgentRecommendRequest request, List<Shop> shops, List<String> references) {
        LlmRequest llmRequest = new LlmRequest();
        llmRequest.setScene("recommend-agent");
        llmRequest.setSystemPrompt(promptTemplateService.load("recommend-agent"));
        llmRequest.setUserPrompt("用户需求：" + request.getQuery() +
                "\n候选店铺：" + shops.stream().map(Shop::getName).collect(Collectors.joining(",")) +
                "\n证据：" + String.join(";", references));
        return llmService.chat(llmRequest);
    }

    private <T> T record(List<AgentToolCallLog> logs, String toolName, String arguments, ToolSupplier<T> supplier) {
        long start = System.currentTimeMillis();
        AgentToolCallLog log = new AgentToolCallLog();
        log.setToolName(toolName);
        log.setArguments(arguments);
        try {
            T result = supplier.get();
            log.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.setSuccess(false);
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            log.setDurationMs(System.currentTimeMillis() - start);
            logs.add(log);
        }
    }

    private List<String> toolNames(List<AgentToolCallLog> toolLogs) {
        return toolLogs.stream().map(AgentToolCallLog::getToolName).distinct().collect(Collectors.toList());
    }

    private void logTool(AgentToolCallLog toolLog) {
        log.info("agent_tool toolName={}, arguments={}, durationMs={}, success={}",
                toolLog.getToolName(), toolLog.getArguments(), toolLog.getDurationMs(), toolLog.getSuccess());
    }

    private interface ToolSupplier<T> {
        T get();
    }
}
