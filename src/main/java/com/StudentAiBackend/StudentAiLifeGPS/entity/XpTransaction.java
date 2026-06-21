package com.StudentAiBackend.StudentAiLifeGPS.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "xp_transactions")
@Data
public class XpTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String action;
    private int amount;
    private String metadata;
    private LocalDateTime createdAt = LocalDateTime.now();
}
