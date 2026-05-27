package com.KryptoChat.serwer.repositories;

import com.KryptoChat.serwer.entities.GroupKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupKeyRepository extends JpaRepository<GroupKey, Long> {

    Optional<GroupKey> findByGroupIdAndUserId(Long groupId, Long userId);
}