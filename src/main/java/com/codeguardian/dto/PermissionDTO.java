package com.codeguardian.dto; import lombok.*; @Data @Builder @NoArgsConstructor @AllArgsConstructor public class PermissionDTO{Long id; String code,name,description,resource,action;}
