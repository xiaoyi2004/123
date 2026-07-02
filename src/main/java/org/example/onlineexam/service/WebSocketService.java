package org.example.onlineexam.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 向指定用户推送通知（用户订阅 /user/queue/notifications）
    public void sendToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", payload);
    }

    // 向所有用户广播（如系统公告）
    public void sendToAll(Object payload) {
        messagingTemplate.convertAndSend("/topic/public", payload);
    }
}