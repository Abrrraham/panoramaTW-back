package com.panorama.backend.service.auth;

import com.panorama.backend.model.auth.UserAccount;
import com.panorama.backend.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepo userAccountRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new User(account.getUsername(), account.getPassword(), account.isEnabled(),
                true, true, true,
                account.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        .toList());
    }
}
