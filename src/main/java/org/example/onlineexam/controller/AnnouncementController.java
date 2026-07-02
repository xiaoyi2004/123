package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.Announcement;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.AnnouncementRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/announcement")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/list")
    public String list(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        // 所有用户均可查看，但只展示已发布
        List<Announcement> list = announcementRepository.findByStatusOrderByCreatedAtDesc(1);
        model.addAttribute("announcements", list);
        return "announcement_student";
    }
}