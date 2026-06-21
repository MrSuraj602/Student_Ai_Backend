package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_messages")
@Data
public class ModuleMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long moduleId;
    private Long userId;
    
    private String role; // user or assistant
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private LocalDateTime createdAt;
}
