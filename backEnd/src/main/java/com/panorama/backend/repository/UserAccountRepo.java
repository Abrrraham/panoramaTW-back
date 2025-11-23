package com.panorama.backend.repository;

import com.panorama.backend.model.auth.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepo extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByUsername(String username);
}
