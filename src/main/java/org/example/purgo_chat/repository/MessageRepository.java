package org.example.purgo_chat.repository;

import org.example.purgo_chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Integer chatRoomId);
}