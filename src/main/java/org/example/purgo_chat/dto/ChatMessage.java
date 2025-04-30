package org.example.purgo_chat.dto;

// ChatMessage.java - 클라이언트와 서버 간 메시지 교환을 위한 DTO
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private MessageType type;
    private String roomId;
    private String sender;
    private String receiver;
    private String content;
    private String time;

    public enum MessageType {
        ENTER, TALK, LEAVE
    }
}