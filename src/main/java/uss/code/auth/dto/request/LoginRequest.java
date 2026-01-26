package uss.code.auth.dto.request;

public record LoginRequest(
        String studentId,
        String password
) {}
