package org.example.purgo_chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purgo_chat.dto.ChatMessage;
import org.example.purgo_chat.dto.FilterResponse;
import org.example.purgo_chat.entity.ChatRoom;
import org.example.purgo_chat.service.BadwordFilterService;
import org.example.purgo_chat.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatService chatService;
    private final BadwordFilterService badwordFilterService;

    // 사용자 세션 관리 맵 (사용자 이름 -> 웹소켓 세션)
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 채팅방 ID 관리 맵 (사용자 이름 -> 채팅방 ID)
    private final Map<String, Integer> userChatRoomIds = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("웹소켓 연결 성립: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("수신 메시지: {}", payload);

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        switch (chatMessage.getType()) {
            case ENTER:
                handleEnterMessage(session, chatMessage);
                break;
            case TALK:
                handleTalkMessage(chatMessage);
                break;
            case LEAVE:
                handleLeaveMessage(chatMessage);
                break;
        }
    }

    private void handleEnterMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        String senderName = chatMessage.getSender();
        String receiverName = chatMessage.getReceiver();

        // 채팅방 생성 또는 조회
        ChatRoom chatRoom = chatService.getChatRoom(senderName, receiverName);

        // 현재 사용자의 세션 저장
        userSessions.put(senderName, session);
        userChatRoomIds.put(senderName, chatRoom.getId());

        // 입장 메시지를 보낸 클라이언트에게 채팅방 ID 전달
        chatMessage.setRoomId(chatRoom.getId().toString());
        chatMessage.setTime(getCurrentTime());
        chatMessage.setContent(senderName + "님이 입장하셨습니다.");

        TextMessage responseMessage = new TextMessage(objectMapper.writeValueAsString(chatMessage));
        session.sendMessage(responseMessage);

        // 상대방이 접속 중이면 입장 메시지 전달
        WebSocketSession receiverSession = userSessions.get(receiverName);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(responseMessage);
        }
    }

    private void handleTalkMessage(ChatMessage chatMessage) throws IOException {
        String senderName = chatMessage.getSender();
        String receiverName = chatMessage.getReceiver();
        String content = chatMessage.getContent();

        // 채팅방 ID로 채팅방 조회
        Integer chatRoomId = Integer.parseInt(chatMessage.getRoomId());
        ChatRoom chatRoom = chatService.getChatRoom(senderName, receiverName);

        // 욕설 필터링 요청
        FilterResponse filterResponse = badwordFilterService.filterMessage(content, chatRoom, senderName);

        // 필터링된 메시지로 업데이트
        String filteredContent = filterResponse.getRewrittenText();
        if (filterResponse.isAbusive()) {
            log.info("욕설 감지: 원본={}, 필터링={}", content, filteredContent);
        }

        // 메시지 DB 저장
        chatService.saveMessage(chatRoom, senderName, receiverName, filteredContent);

        // 메시지 전송 시간 설정
        chatMessage.setContent(filteredContent);
        chatMessage.setTime(getCurrentTime());

        // 메시지를 JSON으로 변환
        String messageJson = objectMapper.writeValueAsString(chatMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // 발신자에게 메시지 전송
        WebSocketSession senderSession = userSessions.get(senderName);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(textMessage);
        }

        // 수신자에게 메시지 전송
        WebSocketSession receiverSession = userSessions.get(receiverName);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(textMessage);
        }
    }

    private void handleLeaveMessage(ChatMessage chatMessage) throws IOException {
        String senderName = chatMessage.getSender();
        String receiverName = chatMessage.getReceiver();
        Integer chatRoomId = Integer.parseInt(chatMessage.getRoomId());

        // 퇴장 카운트 증가
        chatService.incrementLeaveCount(chatRoomId);

        // 사용자 세션 제거
        userSessions.remove(senderName);
        userChatRoomIds.remove(senderName);

        // 퇴장 메시지 설정
        chatMessage.setTime(getCurrentTime());
        chatMessage.setContent(senderName + "님이 퇴장하셨습니다.");

        // 메시지를 JSON으로 변환
        String messageJson = objectMapper.writeValueAsString(chatMessage);
        TextMessage textMessage = new TextMessage(messageJson);

        // 수신자에게 퇴장 메시지 전송
        WebSocketSession receiverSession = userSessions.get(receiverName);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(textMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("웹소켓 연결 종료: {}, 상태: {}", session.getId(), status);

        // 연결이 끊긴 사용자 찾기
        String disconnectedUser = null;
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getValue().getId().equals(session.getId())) {
                disconnectedUser = entry.getKey();
                break;
            }
        }

        // 사용자가 찾아지면 채팅방에서 퇴장 처리
        if (disconnectedUser != null) {
            Integer chatRoomId = userChatRoomIds.get(disconnectedUser);
            if (chatRoomId != null) {
                chatService.incrementLeaveCount(chatRoomId);
                userSessions.remove(disconnectedUser);
                userChatRoomIds.remove(disconnectedUser);
            }
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}