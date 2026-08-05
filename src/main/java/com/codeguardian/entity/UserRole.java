package com.codeguardian.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="user_roles",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","role_id"})) @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor public class UserRole { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="user_id",nullable=false) Long userId; @Column(name="role_id",nullable=false) Long roleId; @Column(nullable=false) LocalDateTime createdAt; @PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();} }
