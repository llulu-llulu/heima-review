package com.hmdp.ai.service;

import com.hmdp.ai.dto.KnowledgeDocument;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class KnowledgeIndexService {

    private final IShopService shopService;
    private final IVoucherService voucherService;
    private final IBlogService blogService;

    public KnowledgeIndexService(IShopService shopService, IVoucherService voucherService, IBlogService blogService) {
        this.shopService = shopService;
        this.voucherService = voucherService;
        this.blogService = blogService;
    }

    public List<KnowledgeDocument> rebuild() {
        List<KnowledgeDocument> documents = new ArrayList<>();
        shopService.list().forEach(shop -> documents.add(fromShop(shop)));
        voucherService.list().forEach(voucher -> documents.add(fromVoucher(voucher)));
        blogService.list().forEach(blog -> documents.add(fromBlog(blog)));
        return documents;
    }

    public List<KnowledgeDocument> search(String question, Long typeId, int limit) {
        String normalizedQuestion = normalize(question);
        return rebuild().stream()
                .filter(document -> typeId == null || !document.getDocType().equals("shop") || document.getTags().contains("type:" + typeId))
                .peek(document -> document.setScore(score(document, normalizedQuestion)))
                .filter(document -> document.getScore() > 0)
                .sorted(Comparator.comparing(KnowledgeDocument::getScore).reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .collect(Collectors.toList());
    }

    private KnowledgeDocument fromShop(Shop shop) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId("shop:" + shop.getId());
        document.setDocType("shop");
        document.setSourceId(shop.getId());
        document.setTitle(shop.getName());
        document.setContent(join(shop.getName(), shop.getArea(), shop.getAddress(), shop.getOpenHours(),
                shop.getScore() == null ? null : "评分" + shop.getScore(),
                shop.getAvgPrice() == null ? null : "人均" + shop.getAvgPrice()));
        document.setTags(Arrays.asList("shop", "type:" + shop.getTypeId(), value(shop.getArea())));
        return document;
    }

    private KnowledgeDocument fromVoucher(Voucher voucher) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId("voucher:" + voucher.getId());
        document.setDocType("voucher");
        document.setSourceId(voucher.getId());
        document.setTitle(voucher.getTitle());
        document.setContent(join(voucher.getTitle(), voucher.getSubTitle(), voucher.getRules(), "shop:" + voucher.getShopId()));
        document.setTags(Arrays.asList("voucher", "shop:" + voucher.getShopId(), "优惠"));
        return document;
    }

    private KnowledgeDocument fromBlog(Blog blog) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId("blog:" + blog.getId());
        document.setDocType("blog");
        document.setSourceId(blog.getId());
        document.setTitle(blog.getTitle());
        document.setContent(join(blog.getTitle(), blog.getContent(), "shop:" + blog.getShopId()));
        document.setTags(Arrays.asList("blog", "shop:" + blog.getShopId(), "探店"));
        return document;
    }

    private double score(KnowledgeDocument document, String normalizedQuestion) {
        String text = normalize(join(document.getTitle(), document.getContent(), String.join(" ", document.getTags())));
        double score = 0;
        for (String token : normalizedQuestion.split("\\s+")) {
            if (StringUtils.hasText(token) && text.contains(token)) {
                score += 1;
            }
        }
        if (text.contains(normalizedQuestion)) {
            score += 2;
        }
        return score;
    }

    private String join(String... values) {
        return Arrays.stream(values).filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[，。！？,.!?]", " ").trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
