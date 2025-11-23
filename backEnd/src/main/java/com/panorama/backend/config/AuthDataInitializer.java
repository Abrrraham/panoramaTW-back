package com.panorama.backend.config;

import com.panorama.backend.model.auth.Role;
import com.panorama.backend.model.auth.UserAccount;
import com.panorama.backend.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataInitializer implements CommandLineRunner {

    private final UserAccountRepo userAccountRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userAccountRepo.findByUsername("admin").ifPresentOrElse(
                user -> log.info("Admin account already exists"),
                () -> {
                    UserAccount admin = UserAccount.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("Admin@123"))
                            .roles(EnumSet.of(Role.ROLE_ADMIN))
                            .enabled(true)
                            .build();
                    userAccountRepo.save(admin);
                    log.info("Initialized default admin account (username: admin / password: Admin@123). Please change it asap.");
                }
        );
    }
}
