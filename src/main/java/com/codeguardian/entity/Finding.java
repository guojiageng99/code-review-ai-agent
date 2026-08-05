package com.codeguardian.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="findings") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Finding {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id", nullable=false) private ReviewTask task;
    @Column(nullable=false) private String severity;
    @Column(nullable=false,columnDefinition="TEXT") private String title;
    @Column(nullable=false,columnDefinition="TEXT") private String location;
    private Integer startLine; private Integer endLine;
    @Column(nullable=false,columnDefinition="TEXT") private String description;
    @Column(columnDefinition="TEXT") private String suggestion;
    private String category;
    @Column(nullable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
}
