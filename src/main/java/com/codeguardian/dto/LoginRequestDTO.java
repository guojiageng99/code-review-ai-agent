package com.codeguardian.dto;
import jakarta.validation.constraints.NotBlank; import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class LoginRequestDTO { @NotBlank(message="Username or email must not be blank") private String usernameOrEmail; @NotBlank(message="Password must not be blank") private String password; }
