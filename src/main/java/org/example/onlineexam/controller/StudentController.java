package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.example.onlineexam.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final AnnouncementRepository announcementRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final NotificationService notificationService;

    public StudentController(ExamRepository examRepository,
                             ExamResultRepository examResultRepository,
                             AnnouncementRepository announcementRepository,
                             StudentAnswerRepository studentAnswerRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             QuestionRepository questionRepository,
                             NotificationService notificationService) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.announcementRepository = announcementRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.notificationService = notificationService;
    }

    private User currentStudent(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return (user == null || !"student".equals(user.getRole())) ? null : user;
    }

    private boolean classMatched(String classNames, String studentClass) {
        if (classNames == null || classNames.isBlank()) return true;
        return Arrays.stream(classNames.split(",")).map(String::trim)
                .anyMatch(c -> c.equals(studentClass));
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        LocalDateTime now = LocalDateTime.now();
        List<Exam> availableExams = examRepository.findAll().stream()
                .filter(e -> "进行中".equals(e.getStatus())
                        && (e.getStartTime() == null || now.isAfter(e.getStartTime()))
                        && (e.getEndTime() == null || now.isBefore(e.getEndTime()))
                        && classMatched(e.getClassNames(), user.getClassName()))
                .collect(Collectors.toList());

        // 未读通知数
        long unreadCount = notificationService.getUnreadCount(user.getId());

        model.addAttribute("student", user);
        model.addAttribute("exams", availableExams);
        model.addAttribute("unreadCount", unreadCount);
        return "student_dashboard";
    }

    @GetMapping("/results")
    public String myResults(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";
        List<ExamResult> results = examResultRepository.findByStudentId(user.getId());
        Map<Long, Exam> examMap = examRepository.findAll().stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));
        model.addAttribute("student", user);
        model.addAttribute("results", results);
        model.addAttribute("examMap", examMap);
        return "result";
    }

    @GetMapping("/wrong-questions")
    public String wrongQuestions(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        List<ExamResult> results = examResultRepository.findByStudentId(user.getId());
        List<Map<String, Object>> wrongList = new ArrayList<>();

        for (ExamResult r : results) {
            if (!"已批阅".equals(r.getGradeStatus())) continue;
            Exam exam = examRepository.findById(r.getExamId()).orElse(null);
            if (exam == null) continue;
            if (exam.getShowAnalysis() == null || exam.getShowAnalysis() == 0) continue;

            List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
            for (PaperQuestion pq : pqs) {
                Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
                if (q == null) continue;
                StudentAnswer sa = studentAnswerRepository.findByStudentIdAndExamIdAndQuestionId(
                        user.getId(), r.getExamId(), q.getId());
                if (sa == null) continue;
                boolean isWrong = false;
                if ("essay".equals(q.getType()) || "analysis".equals(q.getType()) || "programming".equals(q.getType())) {
                    Integer fullScore = pq.getScore();
                    Integer gotScore = sa.getScore();
                    if (gotScore == null || gotScore < fullScore) isWrong = true;
                } else {
                    if (sa.getIsCorrect() == null || sa.getIsCorrect() == 0) isWrong = true;
                }
                if (isWrong) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", q);
                    item.put("studentAnswer", sa.getAnswer());
                    item.put("correctAnswer", q.getAnswer());
                    item.put("analysis", q.getAnalysis());
                    item.put("examName", exam.getName());
                    wrongList.add(item);
                }
            }
        }
        model.addAttribute("wrongList", wrongList);
        return "wrong_questions";
    }

    @GetMapping("/announcements")
    public String announcements(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";
        List<Announcement> list = announcementRepository.findByStatusOrderByCreatedAtDesc(1);
        model.addAttribute("announcements", list);
        return "announcement_student";
    }
}