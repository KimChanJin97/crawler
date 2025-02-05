package com.rouleatt.demo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.regex.Matcher;
import lombok.Builder;

@Builder
public record ReviewDto(
        String restaurantId,
        String reviewerName,
        String typeName,
        String url,
        String thumbnailUrl,
        String title,
        Long reviewIndex,
        String content,
        String createdAt
) {
    public static ArrayList<ReviewDto> parseAndInitReviewDtos(ObjectMapper objectMapper, String restaurantId, Matcher reviewMatcher) {

        ArrayList<ReviewDto> reviewDtos = new ArrayList<>();

        try {
            while (reviewMatcher.find()) {
                String node = reviewMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode reviewNode = objectMapper.readTree(node);
                reviewDtos.add(ReviewDto.builder()
                        .restaurantId(restaurantId)
                        .reviewerName(reviewNode.path("name").asText())
                        .typeName(reviewNode.path("typeName").asText())
                        .url(reviewNode.path("url").asText())
                        .thumbnailUrl(reviewNode.path("thumbnailUrl").asText())
                        .title(reviewNode.path("title").asText())
                        .reviewIndex(reviewNode.path("rank").asLong())
                        .content(reviewNode.path("contents").asText())
                        .createdAt(reviewNode.path("createdString").asText())
                        .build());
            }
        } catch (JsonProcessingException e) {
            // 리뷰 존재하지 않다면 파싱 에러 무시
        }
        return reviewDtos;
    }
}
