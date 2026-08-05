package com.codeguardian.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class LoginResponseDTO { private Boolean success; private String message; private Long userId; private String username; private String realName; private String token; }
