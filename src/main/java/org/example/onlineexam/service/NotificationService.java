package org.example.onlineexam.service;

import org.example.onlineexam.entity.Notification;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.entity.UserNotification;
import org.example.onlineexam.repository.NotificationRepository;
import org.example.onlineexam.repository.UserNotificationRepository;
import org.example.onlineexam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserNotificationRepository userNotificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userRepository = userRepository;
    }

    // 发送通知给指定用户（可多个）
    @Transactional
    public Notification sendNotification(String title, String content, String type, String targetUrl,
                                         Long senderId, List<Long> userIds) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setTargetUrl(targetUrl);
        notification.setSenderId(senderId);
        notification.setSentAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        // 关联用户
        for (Long uid : userIds) {
            UserNotification un = new UserNotification();
            un.setUserId(uid);
            un.setNotificationId(notification.getId());
            un.setIsRead(0);
            un.setCreatedAt(LocalDateTime.now());
            userNotificationRepository.save(un);
        }
        return notification;
    }

    // 发送给所有学生（例如成绩发布）
    public void sendToAllStudents(Notification notification) {
        List<User> students = userRepository.findByRole("student");
        List<Long> userIds = students.stream().map(User::getId).collect(java.util.stream.Collectors.toList());
        sendNotification(notification.getTitle(), notification.getContent(), notification.getType(),
                notification.getTargetUrl(), notification.getSenderId(), userIds);
    }

    // 获取用户未读数量
    public long getUnreadCount(Long userId) {
        return userNotificationRepository.countByUserIdAndIsRead(userId, 0);
    }

    // 获取用户所有通知（含已读）
    public List<UserNotification> getUserNotifications(Long userId) {
        return userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 标记为已读
    public void markAsRead(Long userNotificationId) {
        userNotificationRepository.findById(userNotificationId).ifPresent(un -> {
            un.setIsRead(1);
            un.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(un);
        });
    }
}