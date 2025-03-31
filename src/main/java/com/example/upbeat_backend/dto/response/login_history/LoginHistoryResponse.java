package com.example.upbeat_backend.dto.response.login_history;

import com.example.upbeat_backend.model.enums.LoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryResponse {
    private String date;
    private String time;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String os;
    private LoginStatus status;
    private String failureReason;
}
