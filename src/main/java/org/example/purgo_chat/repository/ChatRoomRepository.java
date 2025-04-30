package org.example.purgo_chat.repository;

import org.example.purgo_chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
    Optional<ChatRoom> findByUser1NameAndUser2Name(String user1Name, String user2Name);
    Optional<ChatRoom> findByUser2NameAndUser1Name(String user1Name, String user2Name);
}