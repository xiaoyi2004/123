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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/exam")
public class ExamController {
    private final ExamRepository examRepository; private final PaperQuestionRepository paperQuestionRepository; private final QuestionRepository questionRepository; private final ExamResultRepository examResultRepository; private final GradingService gradingService; private final ObjectMapper objectMapper = new ObjectMapper();
    public ExamController(ExamRepository examRepository, PaperQuestionRepository paperQuestionRepository, QuestionRepository questionRepository, ExamResultRepository examResultRepository, GradingService gradingService) { this.examRepository = examRepository; this.paperQuestionRepository = paperQuestionRepository; this.questionRepository = questionRepository; this.examResultRepository = examResultRepository; this.gradingService = gradingService; }
    private User currentStudent(HttpSession session) { User user = (User) session.getAttribute("user"); return user == null || !"student".equals(user.getRole()) ? null : user; }

    @GetMapping("/{examId}")
    public String takeExam(@PathVariable Long examId, HttpSession session, Model model) {
        User user = currentStudent(session); if (user == null) return "redirect:/login";
        Exam exam = examRepository.findById(examId).orElse(null); if (exam == null) return "redirect:/student/dashboard?error=examNotFound";
        if (!classMatched(exam.getClassNames(), user.getClassName())) return "redirect:/student/dashboard?error=classDenied";
        if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) return "redirect:/student/results?msg=alreadyTaken";
        LocalDateTime now = LocalDateTime.now(); if (exam.getStartTime()!=null && now.isBefore(exam.getStartTime())) return "redirect:/student/dashboard?msg=notStarted"; if (exam.getEndTime()!=null && now.isAfter(exam.getEndTime())) return "redirect:/student/dashboard?msg=expired";
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = pqs.stream().map(pq -> questionRepository.findById(pq.getQuestionId()).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long,Integer> scoreMap = pqs.stream().collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq.getScore()==null?0:pq.getScore()));
        model.addAttribute("exam", exam); model.addAttribute("questions", questions); model.addAttribute("scoreMap", scoreMap); model.addAttribute("student", user); return "take_exam";
    }
    private boolean classMatched(String classNames, String studentClass) { if (classNames == null || classNames.isBlank()) return true; return Arrays.stream(classNames.split(",")).map(String::trim).anyMatch(c -> c.equals(studentClass)); }

    @PostMapping("/{examId}/submit")
    public String submitExam(@PathVariable Long examId, HttpSession session, HttpServletRequest request) throws JsonProcessingException {
        User user = currentStudent(session); if (user == null) return "redirect:/login";
        if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) return "redirect:/student/results?msg=alreadySubmitted";
        Exam exam = examRepository.findById(examId).orElse(null); if (exam == null) return "redirect:/student/dashboard";
        if (exam.getEndTime()!=null && LocalDateTime.now().isAfter(exam.getEndTime())) return "redirect:/student/dashboard?msg=timeExpired";
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = new ArrayList<>(); Map<Long,Integer> scoreMap = new HashMap<>(); Map<String,String> answers = new LinkedHashMap<>();
        for (PaperQuestion pq : pqs) { Question q = questionRepository.findById(pq.getQuestionId()).orElse(null); if (q != null) { questions.add(q); scoreMap.put(q.getId(), pq.getScore()==null?0:pq.getScore()); answers.put(String.valueOf(q.getId()), Optional.ofNullable(request.getParameter("q_" + q.getId())).orElse("")); } }
        int objective = gradingService.gradeObjective(questions, scoreMap, answers); boolean hasSubjective = gradingService.hasSubjective(questions);
        ExamResult result = new ExamResult(); result.setStudentId(user.getId()); result.setExamId(examId); result.setObjectiveScore(objective); result.setSubjectiveScore(0); result.setScore(objective); result.setGradeStatus(hasSubjective ? "待批阅" : "已批阅"); result.setAnswersJson(objectMapper.writeValueAsString(answers)); result.setSubmitTime(LocalDateTime.now()); examResultRepository.save(result);
        return "redirect:/student/results";
    }
}
