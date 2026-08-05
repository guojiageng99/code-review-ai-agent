package com.codeguardian.controller;
import com.codeguardian.dto.*; import com.codeguardian.entity.ReviewTask; import com.codeguardian.repository.ReviewTaskRepository; import com.codeguardian.service.ReviewService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.data.domain.*; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/review") @RequiredArgsConstructor @CrossOrigin(origins="*") public class ReviewController {
 private final ReviewService service; private final ReviewTaskRepository tasks;
 private ResponseEntity<ReviewResponseDTO> create(ReviewRequestDTO r,String type){r.setReviewType(type); return ResponseEntity.ok(service.createReviewTask(r));}
 @PostMapping("/snippet") public ResponseEntity<ReviewResponseDTO> snippet(@Valid @RequestBody ReviewRequestDTO r){return create(r,"SNIPPET");}
 @PostMapping("/file") public ResponseEntity<ReviewResponseDTO> file(@Valid @RequestBody ReviewRequestDTO r){return create(r,"FILE");}
 @PostMapping("/directory") public ResponseEntity<ReviewResponseDTO> directory(@Valid @RequestBody ReviewRequestDTO r){return create(r,"DIRECTORY");}
 @PostMapping("/project") public ResponseEntity<ReviewResponseDTO> project(@Valid @RequestBody ReviewRequestDTO r){return create(r,"PROJECT");}
 @GetMapping("/task/{id}") public ResponseEntity<ReviewTask> task(@PathVariable Long id){return ResponseEntity.ok(service.getTaskById(id));}
 @GetMapping("/task/{id}/findings") public ResponseEntity<List<FindingDTO>> findings(@PathVariable Long id){return ResponseEntity.ok(service.getFindingsByTaskId(id));}
 @GetMapping("/history") public ResponseEntity<Page<ReviewTask>> history(@RequestParam(required=false) String name,@RequestParam(required=false) String reviewType,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(tasks.findByConditions(name,reviewType,PageRequest.of(page,size,Sort.by("createdAt").descending())));}
}
