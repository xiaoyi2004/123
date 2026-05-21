package org.example.onlineexam.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.example.onlineexam.service.GradingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/exam")
public class ExamController {
    private final ExamRepository examRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamResultRepository examResultRepository;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExamController(ExamRepository examRepository,
                          PaperQuestionRepository paperQuestionRepository,
                          QuestionRepository questionRepository,
                          ExamResultRepository examResultRepository,
                          GradingService gradingService) {
        this.examRepository = examRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.examResultRepository = examResultRepository;
        this.gradingService = gradingService;
    }

    private User currentStudent(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"student".equals(user.getRole())) return null;
        return user;
    }

    @GetMapping("/{examId}")
    public String takeExam(@PathVariable Long examId, HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return "redirect:/student/dashboard?error=examNotFound";

        // 检查是否已考过
        if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) {
            return "redirect:/student/results?msg=alreadyTaken";
        }

        // 检查考试时间
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return "redirect:/student/dashboard?msg=notStarted";
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "redirect:/student/dashboard?msg=expired";
        }

        List<PaperQuestion> pqList = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = new ArrayList<>();
        for (PaperQuestion pq : pqList) {
            questions.add(questionRepository.findById(pq.getQuestionId()).orElse(null));
        }
        model.addAttribute("exam", exam);
        model.addAttribute("questions", questions);
        return "take_exam";
    }

    @PostMapping("/{examId}/submit")
    public String submitExam(@PathVariable Long examId, HttpSession session, HttpServletRequest request) throws JsonProcessingException {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        // 重复提交检查
        if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) {
            return "redirect:/student/results?msg=alreadySubmitted";
        }

        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return "redirect:/student/dashboard";

        // 时间检查
        LocalDateTime now = LocalDateTime.now();
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "redirect:/student/dashboard?msg=timeExpired";
        }

        List<PaperQuestion> pqList = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = new ArrayList<>();
        Map<String, String> answers = new LinkedHashMap<>();
        for (PaperQuestion pq : pqList) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null) {
                questions.add(q);
                String paramValue = request.getParameter("q_" + q.getId());
                answers.put(String.valueOf(q.getId()), paramValue == null ? "" : paramValue);
            }
        }

        int score = gradingService.grade(questions, answers);
        ExamResult result = new ExamResult();
        result.setStudentId(user.getId());
        result.setExamId(examId);
        result.setScore(score);
        result.setAnswersJson(objectMapper.writeValueAsString(answers));
        result.setSubmitTime(LocalDateTime.now());
        examResultRepository.save(result);
        return "redirect:/student/results";
    }
}