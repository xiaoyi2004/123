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
import java.util.stream.Collectors;

@Controller
public class ResultDetailController {
    private final ExamResultRepository examResultRepository; private final ExamRepository examRepository; private final PaperQuestionRepository paperQuestionRepository; private final QuestionRepository questionRepository; private final UserRepository userRepository; private final ObjectMapper objectMapper = new ObjectMapper();
    public ResultDetailController(ExamResultRepository examResultRepository, ExamRepository examRepository, PaperQuestionRepository paperQuestionRepository, QuestionRepository questionRepository, UserRepository userRepository) { this.examResultRepository = examResultRepository; this.examRepository = examRepository; this.paperQuestionRepository = paperQuestionRepository; this.questionRepository = questionRepository; this.userRepository = userRepository; }

    @GetMapping("/result/detail/{resultId}")
    public String viewResultDetail(@PathVariable Long resultId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); if (user == null) return "redirect:/login";
        ExamResult result = examResultRepository.findById(resultId).orElse(null); if (result == null) return "redirect:/student/results";
        if ("student".equals(user.getRole()) && !result.getStudentId().equals(user.getId())) return "redirect:/student/results";
        Exam exam = examRepository.findById(result.getExamId()).orElse(null); if (exam == null) return "redirect:/student/results";
        Map<String,String> studentAnswers = new HashMap<>(); Map<String,Integer> subjectiveScores = new HashMap<>();
        try { if (result.getAnswersJson()!=null) studentAnswers = objectMapper.readValue(result.getAnswersJson(), new TypeReference<>(){}); } catch (Exception ignored) {}
        try { if (result.getSubjectiveScoresJson()!=null) subjectiveScores = objectMapper.readValue(result.getSubjectiveScoresJson(), new TypeReference<>(){}); } catch (Exception ignored) {}
        Map<Long,Integer> scoreMap = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId()).stream().collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq.getScore()==null?0:pq.getScore()));
        List<Map<String,Object>> details = new ArrayList<>();
        for (PaperQuestion pq : paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId())) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null); if (q == null) continue;
            Map<String,Object> item = new HashMap<>(); String ans = studentAnswers.getOrDefault(String.valueOf(q.getId()), "");
            boolean subjective = "essay".equals(q.getType()); boolean isCorrect = !subjective && ans.trim().equalsIgnoreCase(q.getAnswer()==null?"":q.getAnswer().trim());
            int gotScore = subjective ? subjectiveScores.getOrDefault(String.valueOf(q.getId()), 0) : (isCorrect ? scoreMap.getOrDefault(q.getId(),0) : 0);
            item.put("question", q); item.put("studentAnswer", ans); item.put("correctAnswer", q.getAnswer()); item.put("isCorrect", isCorrect); item.put("score", gotScore); item.put("fullScore", scoreMap.getOrDefault(q.getId(),0)); item.put("subjective", subjective); details.add(item);
        }
        model.addAttribute("result", result); model.addAttribute("exam", exam); model.addAttribute("student", userRepository.findById(result.getStudentId()).orElse(null)); model.addAttribute("details", details); return "result_detail";
    }
}
