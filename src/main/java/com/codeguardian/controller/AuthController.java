package com.codeguardian.controller;
import com.codeguardian.dto.*; import com.codeguardian.service.AuthService; import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity; import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.*; import org.springframework.ui.Model; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/auth") @RequiredArgsConstructor public class AuthController { private final AuthService auth;
 @GetMapping("/login") public String loginPage(){return "login";}
 @PostMapping("/login") public String login(@Valid LoginRequestDTO r,HttpServletRequest req,RedirectAttributes attrs){var out=auth.login(r,req); if(Boolean.TRUE.equals(out.getSuccess())){attrs.addFlashAttribute("message","Login successful"); return "redirect:/";} attrs.addFlashAttribute("error",out.getMessage()); return "redirect:/auth/login";}
 @PostMapping("/login/api") @ResponseBody public ResponseEntity<LoginResponseDTO> loginApi(@Valid @RequestBody LoginRequestDTO r,HttpServletRequest req){return ResponseEntity.ok(auth.login(r,req));}
 @PostMapping("/api/auth/login") @ResponseBody public ResponseEntity<LoginResponseDTO> apiLogin(@Valid @RequestBody LoginRequestDTO r,HttpServletRequest req){return ResponseEntity.ok(auth.login(r,req));}
 @PostMapping("/api/auth/logout") @ResponseBody public ResponseEntity<Void> logout(){cn.dev33.satoken.stp.StpUtil.logout();return ResponseEntity.ok().build();}
}
