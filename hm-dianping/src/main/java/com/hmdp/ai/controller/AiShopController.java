package com.hmdp.ai.controller;

import com.hmdp.ai.dto.ShopAskRequest;
import com.hmdp.ai.service.ShopRagService;
import com.hmdp.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/shop")
public class AiShopController {

    private final ShopRagService shopRagService;

    public AiShopController(ShopRagService shopRagService) {
        this.shopRagService = shopRagService;
    }

    @PostMapping("/ask")
    public Result ask(@RequestBody ShopAskRequest request) {
        if (request == null || shopRagService.invalidQuestion(request.getQuestion())) {
            return Result.fail("question不能为空");
        }
        return Result.ok(shopRagService.ask(request));
    }

    @PostMapping("/reindex")
    public Result reindex() {
        return Result.ok(shopRagService.reindex());
    }
}
