package com.codeguardian.dto;
import jakarta.validation.constraints.NotBlank; import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class ReviewRequestDTO { @NotBlank(message="taskName must not be blank") private String taskName; private String reviewType, codeSnippet, filePath, directoryPath, projectPath, gitUrl, gitUsername, gitPassword, language, modelProvider, ruleTemplate; private Boolean rulesOnly, enableRag; }
