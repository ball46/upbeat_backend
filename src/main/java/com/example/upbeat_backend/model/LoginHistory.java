package com.example.upbeat_backend.model;

import com.example.upbeat_backend.model.enums.LoginStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime loginTime;

    @Column(nullable = false, columnDefinition = "VARCHAR(45)")
    private String ipAddress;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAgent;

    private String deviceType;
    private String browser;
    private String os;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(25)")
    private LoginStatus status;

    private String failureReason;
}
