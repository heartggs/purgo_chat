package org.example.purgo_chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.purgo_chat.entity.ChatRoom;
import org.example.purgo_chat.entity.Message;
import org.example.purgo_chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/room")
    public ResponseEntity<ChatRoom> getChatRoom(
            @RequestParam String user1,
            @RequestParam String user2) {
        ChatRoom chatRoom = chatService.getChatRoom(user1, user2);
        return ResponseEntity.ok(chatRoom);
    }

    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<Message>> getChatHistory(@PathVariable Integer roomId) {
        List<Message> messages = chatService.getChatHistory(roomId);
        return ResponseEntity.ok(messages);
    }
}
