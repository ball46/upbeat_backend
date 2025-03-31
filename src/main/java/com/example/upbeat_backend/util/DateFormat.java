package com.example.upbeat_backend.util;

import com.example.upbeat_backend.exception.auth.LoginHistoryException;

import java.time.LocalDateTime;

public class DateFormat {
    public static LocalDateTime parseDateString(String dateString) {
        try {
            if (dateString.contains("T")) {
                return LocalDateTime.parse(dateString);
            }
            return LocalDateTime.parse(dateString + "T00:00:00");
        } catch (Exception e) {
            throw new LoginHistoryException.InvalidDateFormatException();
        }
    }
}
