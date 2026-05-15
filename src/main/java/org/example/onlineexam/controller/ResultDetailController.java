package org.example.onlineexam.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;

@Controller
public class ResultDetailController {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResultDetailController(ExamResultRepository examResultRepository,
                                  ExamRepository examRepository,
                                  PaperQuestionRepository paperQuestionRepository,
                                  QuestionRepository questionRepository) {
        this.examResultRepository = examResultRepository;
        this.examRepository = examRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
    }

    @GetMapping("/result/detail/{resultId}")
    public String viewResultDetail(@PathVariable Long resultId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Optional<ExamResult> resultOpt = examResultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            return "redirect:/student/results?error=成绩记录不存在";
        }
        ExamResult result = resultOpt.get();
        // 权限检查：学生只能看自己的，教师可以看所有
        if ("student".equals(user.getRole()) && !result.getStudentId().equals(user.getId())) {
            return "redirect:/student/results?error=无权查看他人成绩";
        }

        // 获取考试及试卷题目
        Optional<Exam> examOpt = examRepository.findById(result.getExamId());
        if (examOpt.isEmpty()) return "redirect:/student/results?error=考试信息不存在";
        Exam exam = examOpt.get();

        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            questionRepository.findById(pq.getQuestionId()).ifPresent(questions::add);
        }

        // 解析学生提交的答案
        Map<String, String> studentAnswers = new HashMap<>();
        try {
            if (result.getAnswersJson() != null && !result.getAnswersJson().isEmpty()) {
                studentAnswers = objectMapper.readValue(result.getAnswersJson(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            // ignore
        }

        // 构建题目+学生答案+正确答案+得分明细
        List<Map<String, Object>> details = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new HashMap<>();
            item.put("question", q);
            String studentAnswer = studentAnswers.getOrDefault(String.valueOf(q.getId()), "");
            boolean isCorrect = studentAnswer.trim().equalsIgnoreCase(q.getAnswer() == null ? "" : q.getAnswer().trim());
            item.put("studentAnswer", studentAnswer);
            item.put("correctAnswer", q.getAnswer());
            item.put("isCorrect", isCorrect);
            item.put("score", isCorrect ? q.getScore() : 0);
            details.add(item);
        }

        model.addAttribute("result", result);
        model.addAttribute("exam", exam);
        model.addAttribute("details", details);
        return "result_detail";
    }
}