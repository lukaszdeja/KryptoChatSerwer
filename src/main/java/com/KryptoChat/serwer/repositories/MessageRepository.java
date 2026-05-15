package com.KryptoChat.serwer.repositories;

import com.KryptoChat.serwer.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByGroupId(Long groupId);
}