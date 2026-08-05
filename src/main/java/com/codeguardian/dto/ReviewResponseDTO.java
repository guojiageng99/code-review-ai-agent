package com.codeguardian.dto;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ReviewResponseDTO { private Long taskId; private String taskName, reviewType, status, message; private LocalDateTime createdAt; }
