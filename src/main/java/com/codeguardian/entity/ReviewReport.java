package com.codeguardian.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="review_reports") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewReport {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id", nullable=false, unique=true) private ReviewTask task;
    @Column(columnDefinition="TEXT") private String htmlContent;
    @Column(columnDefinition="TEXT") private String markdownContent;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}
