package com.panorama.backend.controller.auth;

import com.panorama.backend.DTO.auth.AuthRequest;
import com.panorama.backend.DTO.auth.AuthResponse;
import com.panorama.backend.DTO.auth.RefreshRequest;
import com.panorama.backend.DTO.auth.UserProfile;
import com.panorama.backend.model.auth.UserAccount;
import com.panorama.backend.repository.UserAccountRepo;
import com.panorama.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v0/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepo userAccountRepo;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserAccount user = userAccountRepo.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return ResponseEntity.ok(new AuthResponse(token, refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> profile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        UserAccount user = userAccountRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(Enum::name).toList()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String username = jwtService.extractUsername(request.refreshToken());
        UserAccount user = userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return ResponseEntity.ok(new AuthResponse(token, refreshToken));
    }
}
