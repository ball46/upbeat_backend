package com.example.upbeat_backend.repository;

import com.example.upbeat_backend.model.RoleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, String> {
    Page<RoleAuditLog> findByRoleId(String roleId, Pageable pageable);
    Page<RoleAuditLog> findByUserId(String userId, Pageable pageable);
}
