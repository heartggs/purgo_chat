package org.example.purgo_chat.service;

import lombok.RequiredArgsConstructor;
import org.example.purgo_chat.entity.ChatRoom;
import org.example.purgo_chat.entity.Message;
import org.example.purgo_chat.repository.ChatRoomRepository;
import org.example.purgo_chat.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    public ChatRoom getChatRoom(String user1, String user2) {
        // 두 사용자 간의 채팅방 찾기 (순서 무관)
        Optional<ChatRoom> chatRoom = chatRoomRepository.findByUser1NameAndUser2Name(user1, user2);
        if (chatRoom.isPresent()) {
            return chatRoom.get();
        }

        chatRoom = chatRoomRepository.findByUser2NameAndUser1Name(user1, user2);
        if (chatRoom.isPresent()) {
            return chatRoom.get();
        }

        // 존재하지 않으면 새로운 채팅방 생성
        return chatRoomRepository.save(ChatRoom.builder()
                .user1Name(user1)
                .user2Name(user2)
                .build());
    }

    public void incrementLeaveCount(Integer chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        chatRoom.setLeaveCount(chatRoom.getLeaveCount() + 1);
        chatRoomRepository.save(chatRoom);
    }

    public void saveMessage(ChatRoom chatRoom, String senderName, String receiverName, String content) {
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderName(senderName)
                .receiverName(receiverName)
                .content(content)
                .build();

        messageRepository.save(message);
    }

    public List<Message> getChatHistory(Integer chatRoomId) {
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
    }

    public void incrementBadwordCount(ChatRoom chatRoom) {
        chatRoom.setBadwordCount(chatRoom.getBadwordCount() + 1);
        chatRoomRepository.save(chatRoom);
    }
}