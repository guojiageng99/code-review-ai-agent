package com.codeguardian.exception;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.bind.MethodArgumentNotValidException; import java.util.*;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){Map<String,String> errors=new HashMap<>(); e.getBindingResult().getFieldErrors().forEach(x->errors.put(x.getField(),x.getDefaultMessage())); return ResponseEntity.badRequest().body(Map.of("error","Validation failed","errors",errors,"status",400));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,Object>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error","Bad request","message",e.getMessage(),"status",400));}
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> error(Exception e){return ResponseEntity.status(500).body(Map.of("error","Internal server error","message",String.valueOf(e.getMessage()),"status",500));}
}
