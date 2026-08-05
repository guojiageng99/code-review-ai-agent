package com.codeguardian.controller;
import com.codeguardian.dto.*; import com.codeguardian.entity.ReviewTask; import com.codeguardian.repository.ReviewTaskRepository; import com.codeguardian.service.*; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.data.domain.*; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/review") @RequiredArgsConstructor @CrossOrigin(origins="*") public class ReviewController {
 private final ReviewService service; private final ReviewTaskRepository tasks; private final GitService gitService;
 @PostMapping("/git/clone") public ResponseEntity<Map<String,Object>> cloneGit(@RequestBody ReviewRequestDTO r){try{String path=gitService.cloneRepository(r.getGitUrl(),r.getGitUsername(),r.getGitPassword()); return ResponseEntity.ok(Map.of("success",true,"localPath",path,"fileList",gitService.getFileList(path)));}catch(Exception e){return ResponseEntity.status(500).body(Map.of("success",false,"error",e.getMessage()));}}
 @GetMapping("/git/file") public ResponseEntity<Map<String,Object>> readGit(@RequestParam String path){try{return ResponseEntity.ok(Map.of("success",true,"content",gitService.readFile(path)));}catch(Exception e){return ResponseEntity.status(500).body(Map.of("success",false,"error",e.getMessage()));}}
 private ResponseEntity<ReviewResponseDTO> create(ReviewRequestDTO r,String type){r.setReviewType(type); return ResponseEntity.ok(service.createReviewTask(r));}
 @PostMapping("/snippet") public ResponseEntity<ReviewResponseDTO> snippet(@Valid @RequestBody ReviewRequestDTO r){return create(r,"SNIPPET");}
 @PostMapping("/file") public ResponseEntity<ReviewResponseDTO> file(@Valid @RequestBody ReviewRequestDTO r){return create(r,"FILE");}
 @PostMapping("/directory") public ResponseEntity<ReviewResponseDTO> directory(@Valid @RequestBody ReviewRequestDTO r){return create(r,"DIRECTORY");}
 @PostMapping("/project") public ResponseEntity<ReviewResponseDTO> project(@Valid @RequestBody ReviewRequestDTO r){return create(r,"PROJECT");}
 @PostMapping("/git") public ResponseEntity<ReviewResponseDTO> git(@Valid @RequestBody ReviewRequestDTO r){return create(r,"GIT");}
 @PostMapping("/directory/batch") public ResponseEntity<List<ReviewResponseDTO>> directoryBatch(@RequestParam String path){return ResponseEntity.ok(service.createDirectoryTasks(path,false));}
 @PostMapping("/project/batch") public ResponseEntity<List<ReviewResponseDTO>> projectBatch(@RequestParam String path){return ResponseEntity.ok(service.createDirectoryTasks(path,true));}
 @GetMapping("/task/{id}") public ResponseEntity<ReviewTask> task(@PathVariable Long id){return ResponseEntity.ok(service.getTaskById(id));}
 @GetMapping("/task/{id}/findings") public ResponseEntity<List<FindingDTO>> findings(@PathVariable Long id){return ResponseEntity.ok(service.getFindingsByTaskId(id));}
 @GetMapping("/history") public ResponseEntity<Page<ReviewTask>> history(@RequestParam(required=false) String name,@RequestParam(required=false) String reviewType,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(tasks.findByConditions(name,reviewType,PageRequest.of(page,size,Sort.by("createdAt").descending())));}
}
