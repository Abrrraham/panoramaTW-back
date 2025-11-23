package com.panorama.backend.DTO.auth;

import java.util.List;

public record UserProfile(
        String userId,
        String userName,
        List<String> roles
) {
}
