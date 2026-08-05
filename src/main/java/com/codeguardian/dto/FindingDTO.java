package com.codeguardian.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class FindingDTO { private Long id; private String severity,title,location,description,suggestion,category; private Integer startLine,endLine; }
