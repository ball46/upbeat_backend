package com.example.upbeat_backend.util;

import jakarta.servlet.http.HttpServletRequest;

public class DataHeader {
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Forwarded-For");
        }
        return ipAddress;
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
