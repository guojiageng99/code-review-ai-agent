package com.codeguardian.service;
import com.codeguardian.dto.*; import com.codeguardian.entity.*; import com.codeguardian.repository.*; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime; import java.util.List;
@Service @RequiredArgsConstructor public class ReviewService {
 private final ReviewTaskRepository taskRepository; private final FindingRepository findingRepository;
 @Transactional public ReviewResponseDTO createReviewTask(ReviewRequestDTO r){ ReviewTask t=ReviewTask.builder().name(r.getTaskName()).reviewType(r.getReviewType()).scope(scope(r)).status("PENDING").createdAt(LocalDateTime.now()).build(); t=taskRepository.save(t); return ReviewResponseDTO.builder().taskId(t.getId()).taskName(t.getName()).reviewType(t.getReviewType()).status(t.getStatus()).createdAt(t.getCreatedAt()).message("Review task created").build(); }
 public ReviewTask getTaskById(Long id){ return taskRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Task not found: "+id)); }
 public List<FindingDTO> getFindingsByTaskId(Long id){ return findingRepository.findByTaskId(id).stream().map(f->FindingDTO.builder().id(f.getId()).severity(f.getSeverity()).title(f.getTitle()).location(f.getLocation()).startLine(f.getStartLine()).endLine(f.getEndLine()).description(f.getDescription()).suggestion(f.getSuggestion()).category(f.getCategory()).build()).toList(); }
 private String scope(ReviewRequestDTO r){ if(r.getCodeSnippet()!=null)return r.getCodeSnippet(); if(r.getFilePath()!=null)return r.getFilePath(); if(r.getDirectoryPath()!=null)return r.getDirectoryPath(); return r.getProjectPath(); }
}
