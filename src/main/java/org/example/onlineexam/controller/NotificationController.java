package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.Notification;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.UserRepository;
import org.example.onlineexam.service.NotificationService;
import org.example.onlineexam.service.WebSocketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  WebSocketService webSocketService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
        this.userRepository = userRepository;
    }

    // 学生查看通知列表
    @GetMapping("/list")
    public String list(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        List<org.example.onlineexam.entity.UserNotification> list = notificationService.getUserNotifications(user.getId());
        model.addAttribute("notifications", list);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        return "notification_list";
    }

    // 标记已读
    @PostMapping("/read/{id}")
    public String markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notification/list";
    }

    // 发送通知（教师/管理员）
    @PostMapping("/send")
    @ResponseBody
    public String sendNotification(@RequestParam String title,
                                   @RequestParam String content,
                                   @RequestParam String type,
                                   @RequestParam(required = false) String targetUrl,
                                   @RequestParam(required = false) List<Long> userIds,
                                   HttpSession session) {
        User sender = (User) session.getAttribute("user");
        if (sender == null || (!"teacher".equals(sender.getRole()) && !"admin".equals(sender.getRole()))) {
            return "无权限";
        }
        List<Long> targetUsers = userIds;
        if (targetUsers == null || targetUsers.isEmpty()) {
            // 默认发送给所有学生
            targetUsers = userRepository.findByRole("student").stream().map(User::getId).toList();
        }
        Notification notification = notificationService.sendNotification(
                title, content, type, targetUrl, sender.getId(), targetUsers);
        // 通过WebSocket推送
        for (Long uid : targetUsers) {
            webSocketService.sendToUser(uid, notification);
        }
        return "发送成功";
    }
}