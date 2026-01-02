package com.panorama.backend.service.auth;

import com.panorama.backend.model.auth.Role;
import com.panorama.backend.model.auth.UserAccount;
import com.panorama.backend.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepo userAccountRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        Set<Role> roles = account.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = EnumSet.of(Role.ROLE_USER);
        } else if (!(roles instanceof EnumSet)) {
            roles = EnumSet.copyOf(roles);
        }
        if ("admin".equalsIgnoreCase(account.getUsername())) {
            roles.add(Role.ROLE_ADMIN);
        }
        return new User(account.getUsername(), account.getPassword(), account.isEnabled(),
                true, true, true,
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        .toList());
    }
}
