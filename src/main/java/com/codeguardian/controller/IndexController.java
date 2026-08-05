package com.codeguardian.controller;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.GetMapping; import org.springframework.web.bind.annotation.RestController; import java.util.Map;
@RestController public class IndexController { @GetMapping("/") public ResponseEntity<Map<String,Object>> index(){ return ResponseEntity.ok(Map.of("name","CodeGuardian AI","version","1.0.0","description","Professional code review AI Agent","endpoints",Map.of("health","/actuator/health","api","/api/review"))); } }
