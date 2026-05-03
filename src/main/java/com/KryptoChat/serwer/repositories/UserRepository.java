package com.KryptoChat.serwer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.KryptoChat.serwer.entities.*;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}