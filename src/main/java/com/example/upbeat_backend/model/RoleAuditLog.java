package com.example.upbeat_backend.model;

import com.example.upbeat_backend.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuditLog {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Role role;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
