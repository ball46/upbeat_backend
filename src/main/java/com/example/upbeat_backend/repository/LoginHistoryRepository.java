package com.example.upbeat_backend.repository;

import com.example.upbeat_backend.model.LoginHistory;
import com.example.upbeat_backend.model.enums.LoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(String userId);
    int countByUserIdAndStatusAndLoginTimeAfter(String userId, LoginStatus status, LocalDateTime loginTime);
}
