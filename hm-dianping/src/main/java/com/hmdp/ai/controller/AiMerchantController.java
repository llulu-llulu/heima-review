package com.hmdp.ai.controller;

import com.hmdp.ai.dto.MerchantCopywritingRequest;
import com.hmdp.ai.dto.MerchantOperationAdviceRequest;
import com.hmdp.ai.dto.MerchantReviewSummaryRequest;
import com.hmdp.ai.service.MerchantCopilotService;
import com.hmdp.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/merchant")
public class AiMerchantController {

    private final MerchantCopilotService merchantCopilotService;

    public AiMerchantController(MerchantCopilotService merchantCopilotService) {
        this.merchantCopilotService = merchantCopilotService;
    }

    @PostMapping("/copywriting")
    public Result copywriting(@RequestBody MerchantCopywritingRequest request) {
        if (request == null || request.getShopId() == null) {
            return Result.fail("shopId不能为空");
        }
        if (!merchantCopilotService.existsShop(request.getShopId())) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(merchantCopilotService.generateCopywriting(request));
    }

    @PostMapping("/review-summary")
    public Result reviewSummary(@RequestBody MerchantReviewSummaryRequest request) {
        if (request == null || request.getShopId() == null) {
            return Result.fail("shopId不能为空");
        }
        if (!merchantCopilotService.existsShop(request.getShopId())) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(merchantCopilotService.summarizeReviews(request));
    }

    @PostMapping("/operation-advice")
    public Result operationAdvice(@RequestBody MerchantOperationAdviceRequest request) {
        if (request == null || request.getShopId() == null) {
            return Result.fail("shopId不能为空");
        }
        if (!merchantCopilotService.existsShop(request.getShopId())) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(merchantCopilotService.adviseOperation(request));
    }
}
