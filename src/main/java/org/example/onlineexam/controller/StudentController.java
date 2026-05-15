package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.Exam;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.ExamRepository;
import org.example.onlineexam.repository.ExamResultRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;

    public StudentController(ExamRepository examRepository, ExamResultRepository examResultRepository) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
    }

    private User currentStudent(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"student".equals(user.getRole())) return null;
        return user;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (currentStudent(session) == null) return "redirect:/login";
        // 只显示进行中且当前时间在起止范围内的考试
        LocalDateTime now = LocalDateTime.now();
        List<Exam> availableExams = examRepository.findAll().stream()
                .filter(e -> "进行中".equals(e.getStatus()) &&
                        (e.getStartTime() == null || now.isAfter(e.getStartTime())) &&
                        (e.getEndTime() == null || now.isBefore(e.getEndTime())))
                .collect(Collectors.toList());
        model.addAttribute("exams", availableExams);
        return "student_dashboard";
    }

    @GetMapping("/results")
    public String myResults(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("results", examResultRepository.findByStudentId(user.getId()));
        return "result";
    }
}