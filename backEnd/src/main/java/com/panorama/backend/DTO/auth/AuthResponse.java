package com.panorama.backend.DTO.auth;

public record AuthResponse(
        String token,
        String refreshToken
) {
}
