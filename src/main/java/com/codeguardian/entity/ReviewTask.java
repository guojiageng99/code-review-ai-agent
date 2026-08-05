package com.codeguardian.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="review_tasks") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewTask {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String reviewType;
    @Column(columnDefinition="TEXT") private String scope;
    @Column(nullable=false) private String status;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    @OneToMany(mappedBy="task", cascade=CascadeType.ALL, fetch=FetchType.LAZY) @Builder.Default private List<Finding> findings=new ArrayList<>();
    @Column(columnDefinition="TEXT") private String errorMessage;
    @PrePersist void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(); if(status==null) status="PENDING"; }
}
