package com.panorama.backend.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Simple account model stored in MongoDB.
 */
@Document(collection = "userAccount")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @Builder.Default
    private Set<Role> roles = EnumSet.of(Role.ROLE_USER);

    @Builder.Default
    private boolean enabled = true;

    /** profile fields */
    private String nickName;
    private String gender;
    private String phone;
    private String email;

    /** 1=enabled, 2=disabled for UI */
    @Builder.Default
    private String status = "1";

    /** original password (for admin inspection only; null for imported accounts) */
    private String rawPassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
