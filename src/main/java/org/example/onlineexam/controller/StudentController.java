package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
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

    public StudentController(ExamRepository examRepository,
                             ExamResultRepository examResultRepository,
                             AnnouncementRepository announcementRepository,
                             StudentAnswerRepository studentAnswerRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             QuestionRepository questionRepository) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.announcementRepository = announcementRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
    }

    private User currentStudent(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return (user == null || !"student".equals(user.getRole())) ? null : user;
    }

    // ==================== 学生仪表盘 ====================
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

        model.addAttribute("student", user);
        model.addAttribute("exams", availableExams);
        return "student_dashboard";
    }

    private boolean classMatched(String classNames, String studentClass) {
        if (classNames == null || classNames.isBlank()) return true;
        return Arrays.stream(classNames.split(",")).map(String::trim)
                .anyMatch(c -> c.equals(studentClass));
    }

    // ==================== 我的成绩 ====================
    @GetMapping("/results")
    public String myResults(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";
        List<ExamResult> results = examResultRepository.findByStudentId(user.getId());
        // 按考试名称补充
        Map<Long, Exam> examMap = examRepository.findAll().stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));
        model.addAttribute("student", user);
        model.addAttribute("results", results);
        model.addAttribute("examMap", examMap);
        return "result";
    }

    // ==================== 错题解析 ====================
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
            // 检查是否允许查看解析
            if (exam.getShowAnalysis() == null || exam.getShowAnalysis() == 0) continue;

            List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
            for (PaperQuestion pq : pqs) {
                Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
                if (q == null) continue;
                StudentAnswer sa = studentAnswerRepository.findByStudentIdAndExamIdAndQuestionId(
                        user.getId(), r.getExamId(), q.getId());
                if (sa == null) continue;
                // 判断是否答错
                boolean isWrong = false;
                if ("essay".equals(q.getType())) {
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

    // ==================== 公告查看 ====================
    @GetMapping("/announcements")
    public String announcements(HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";
        List<Announcement> list = announcementRepository.findByStatusOrderByCreatedAtDesc(1);
        model.addAttribute("announcements", list);
        return "announcement_student";
    }
}