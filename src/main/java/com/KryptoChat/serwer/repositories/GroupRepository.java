package com.KryptoChat.serwer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.KryptoChat.serwer.entities.*;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByGroupName(String groupName);
    
    Optional<Group> findByKod(String kod);
}
