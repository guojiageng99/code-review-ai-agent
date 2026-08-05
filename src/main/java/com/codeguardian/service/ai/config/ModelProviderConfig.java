package com.codeguardian.service.ai.config; import lombok.*; @Data @Builder public class ModelProviderConfig{String providerName,baseUrl,apiKey,defaultModel;Boolean enabled;Integer timeout;}
