package org.example.onlineexam.repository;

import org.example.onlineexam.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndNotificationId(Long userId, Long notificationId);
    long countByUserIdAndIsRead(Long userId, Integer isRead);
}