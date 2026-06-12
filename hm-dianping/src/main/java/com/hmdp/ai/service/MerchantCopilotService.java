package com.hmdp.ai.service;

import com.hmdp.ai.dto.LlmRequest;
import com.hmdp.ai.dto.LlmResponse;
import com.hmdp.ai.dto.MerchantCopywritingRequest;
import com.hmdp.ai.dto.MerchantCopywritingResponse;
import com.hmdp.ai.dto.MerchantOperationAdviceRequest;
import com.hmdp.ai.dto.MerchantOperationAdviceResponse;
import com.hmdp.ai.dto.MerchantReviewSummaryRequest;
import com.hmdp.ai.dto.MerchantReviewSummaryResponse;
import com.hmdp.ai.prompt.PromptTemplateService;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantCopilotService {

    private final IShopService shopService;
    private final IVoucherService voucherService;
    private final IBlogService blogService;
    private final LlmService llmService;
    private final PromptTemplateService promptTemplateService;

    public MerchantCopilotService(IShopService shopService,
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

    public MerchantCopywritingResponse generateCopywriting(MerchantCopywritingRequest request) {
        MerchantEvidence evidence = loadEvidence(request.getShopId(), 3);
        LlmResponse llmResponse = callLlm("merchant-copywriting", "merchant-copywriting", evidence,
                "活动类型：" + value(request.getActivityType()) +
                        "\n目标用户：" + value(request.getTargetUser()) +
                        "\n语气：" + value(request.getTone()) +
                        "\n额外要求：" + value(request.getExtraRequirement()));

        MerchantCopywritingResponse response = new MerchantCopywritingResponse();
        response.setTitle(evidence.shop.getName() + "｜" + valueOrDefault(request.getActivityType(), "到店活动"));
        response.setContent(llmResponse.getContent());
        response.setSellingPoints(buildSellingPoints(evidence.shop));
        response.setRiskTips(Arrays.asList("文案必须以真实店铺信息为准", "优惠力度需和实际券配置一致"));
        response.setReferences(evidence.references);
        return response;
    }

    public MerchantReviewSummaryResponse summarizeReviews(MerchantReviewSummaryRequest request) {
        MerchantEvidence evidence = loadEvidence(request.getShopId(), limit(request.getMaxReviews(), 5));
        LlmResponse llmResponse = callLlm("merchant-review-summary", "review-summary", evidence,
                "请基于探店博客和店铺统计生成评论总结。");

        MerchantReviewSummaryResponse response = new MerchantReviewSummaryResponse();
        response.setPositiveSummary(llmResponse.getContent());
        response.setNegativeSummary("当前评论数据有限，建议重点关注排队、服务稳定性和价格感知。");
        response.setCommonKeywords(Arrays.asList("环境", "服务", "性价比", "优惠"));
        response.setImprovementSuggestions(Arrays.asList("沉淀真实用户评论", "优化高峰期接待", "突出优惠券使用规则"));
        response.setReferences(evidence.references);
        return response;
    }

    public MerchantOperationAdviceResponse adviseOperation(MerchantOperationAdviceRequest request) {
        MerchantEvidence evidence = loadEvidence(request.getShopId(), 3);
        LlmResponse llmResponse = callLlm("merchant-operation-advice", "merchant-copywriting", evidence,
                "经营目标：" + value(request.getBusinessGoal()) + "\n预算等级：" + value(request.getBudgetLevel()));

        MerchantOperationAdviceResponse response = new MerchantOperationAdviceResponse();
        response.setAdviceList(Arrays.asList(
                llmResponse.getContent(),
                "围绕高评分卖点设计限时券，提升到店转化",
                "将优质探店内容复用为详情页和社交平台素材"
        ));
        response.setPriority("先优化可直接影响转化的优惠券和店铺内容展示");
        response.setExpectedEffect("提升点击到到店链路的转化效率，并减少用户决策成本");
        response.setRiskTips(Arrays.asList("不要承诺不存在的优惠", "预算较低时优先做内容复用而非大额补贴"));
        response.setReferences(evidence.references);
        return response;
    }

    public boolean existsShop(Long shopId) {
        return shopId != null && shopService.getById(shopId) != null;
    }

    private MerchantEvidence loadEvidence(Long shopId, int blogLimit) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        List<Voucher> vouchers = voucherService.query().eq("shop_id", shopId).list();
        List<Blog> blogs = blogService.query()
                .eq("shop_id", shopId)
                .orderByDesc("liked")
                .last("LIMIT " + limit(blogLimit, 5))
                .list();
        return new MerchantEvidence(shop, safeList(vouchers), safeList(blogs));
    }

    private LlmResponse callLlm(String scene, String templateName, MerchantEvidence evidence, String userRequirement) {
        LlmRequest llmRequest = new LlmRequest();
        llmRequest.setScene(scene);
        llmRequest.setSystemPrompt(promptTemplateService.load(templateName));
        llmRequest.setUserPrompt(buildEvidencePrompt(evidence) + "\n" + userRequirement);
        return llmService.chat(llmRequest);
    }

    private String buildEvidencePrompt(MerchantEvidence evidence) {
        String voucherText = evidence.vouchers.stream()
                .map(voucher -> voucher.getId() + ":" + voucher.getTitle())
                .collect(Collectors.joining("; "));
        String blogText = evidence.blogs.stream()
                .map(blog -> blog.getId() + ":" + blog.getTitle())
                .collect(Collectors.joining("; "));
        return "店铺：" + evidence.shop.getId() + "，" + evidence.shop.getName() +
                "，地址：" + value(evidence.shop.getAddress()) +
                "，评分：" + evidence.shop.getScore() +
                "，人均：" + evidence.shop.getAvgPrice() +
                "\n优惠券：" + voucherText +
                "\n探店内容：" + blogText;
    }

    private List<String> buildSellingPoints(Shop shop) {
        List<String> points = new ArrayList<>();
        points.add("店铺：" + shop.getName());
        if (shop.getScore() != null) {
            points.add("评分：" + shop.getScore() / 10.0);
        }
        if (shop.getAvgPrice() != null) {
            points.add("人均：" + shop.getAvgPrice());
        }
        if (shop.getArea() != null) {
            points.add("商圈：" + shop.getArea());
        }
        return points;
    }

    private int limit(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(value, 20));
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static class MerchantEvidence {
        private final Shop shop;
        private final List<Voucher> vouchers;
        private final List<Blog> blogs;
        private final List<String> references;

        private MerchantEvidence(Shop shop, List<Voucher> vouchers, List<Blog> blogs) {
            this.shop = shop;
            this.vouchers = vouchers;
            this.blogs = blogs;
            this.references = new ArrayList<>();
            this.references.add("shop:" + shop.getId() + ":" + shop.getName());
            vouchers.forEach(voucher -> this.references.add("voucher:" + voucher.getId() + ":" + voucher.getTitle()));
            blogs.forEach(blog -> this.references.add("blog:" + blog.getId() + ":" + blog.getTitle()));
        }
    }
}
