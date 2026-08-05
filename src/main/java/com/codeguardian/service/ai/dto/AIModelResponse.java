package com.codeguardian.service.ai.dto; import lombok.*; @Data @Builder public class AIModelResponse{String content;String model;Integer usageTokens;String finishReason;}
