package com.KryptoChat.serwer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.KryptoChat.serwer.entities.*;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
}