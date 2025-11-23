package com.panorama.backend.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
}
