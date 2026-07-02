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

    private final ExamRepository examRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExamController(ExamRepository examRepository,
                          PaperQuestionRepository paperQuestionRepository,
                          QuestionRepository questionRepository,
                          ExamResultRepository examResultRepository,
                          StudentAnswerRepository studentAnswerRepository,
                          GradingService gradingService) {
        this.examRepository = examRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.examResultRepository = examResultRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.gradingService = gradingService;
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

    // ==================== 进入考试 ====================
    @GetMapping("/{examId}")
    public String takeExam(@PathVariable Long examId, HttpSession session, Model model) {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return "redirect:/student/dashboard?error=examNotFound";

        if (!classMatched(exam.getClassNames(), user.getClassName())) {
            return "redirect:/student/dashboard?error=classDenied";
        }

        // 检查是否已提交（若允许重复考试则跳过）
        if (exam.getAllowRepeat() == null || exam.getAllowRepeat() == 0) {
            if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) {
                return "redirect:/student/results?msg=alreadyTaken";
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return "redirect:/student/dashboard?msg=notStarted";
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "redirect:/student/dashboard?msg=expired";
        }

        // 获取试卷题目
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = pqs.stream()
                .map(pq -> questionRepository.findById(pq.getQuestionId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 如果题目乱序，打乱题目列表（保留题目顺序关系）
        if (exam.getShuffleQuestions() != null && exam.getShuffleQuestions() == 1) {
            Collections.shuffle(questions);
        }

        Map<Long, Integer> scoreMap = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId,
                        pq -> pq.getScore() == null ? 0 : pq.getScore()));

        // 加载已保存的答案（用于恢复）
        List<StudentAnswer> savedAnswers = studentAnswerRepository.findByStudentIdAndExamId(user.getId(), examId);
        Map<Long, String> answerMap = savedAnswers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, StudentAnswer::getAnswer));

        model.addAttribute("exam", exam);
        model.addAttribute("questions", questions);
        model.addAttribute("scoreMap", scoreMap);
        model.addAttribute("student", user);
        model.addAttribute("answerMap", answerMap);
        return "take_exam";
    }

    // ==================== 提交考试 ====================
    @PostMapping("/{examId}/submit")
    public String submitExam(@PathVariable Long examId,
                             HttpSession session,
                             HttpServletRequest request) throws JsonProcessingException {
        User user = currentStudent(session);
        if (user == null) return "redirect:/login";

        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return "redirect:/student/dashboard";

        // 检查是否已提交（若允许重复考试，则允许再次提交但覆盖旧记录？这里简化：若已存在且不允许重复则跳转）
        if (exam.getAllowRepeat() == null || exam.getAllowRepeat() == 0) {
            if (examResultRepository.existsByStudentIdAndExamId(user.getId(), examId)) {
                return "redirect:/student/results?msg=alreadySubmitted";
            }
        }

        // 后端时间校验
        if (exam.getEndTime() != null && LocalDateTime.now().isAfter(exam.getEndTime())) {
            return "redirect:/student/dashboard?msg=timeExpired";
        }

        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId());
        List<Question> questions = new ArrayList<>();
        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<String, String> answers = new LinkedHashMap<>();

        // 先清空旧答案（如果允许多次提交，则覆盖）
        studentAnswerRepository.deleteByStudentIdAndExamId(user.getId(), examId);

        for (PaperQuestion pq : pqs) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) continue;
            questions.add(q);
            scoreMap.put(q.getId(), pq.getScore() == null ? 0 : pq.getScore());
            String studentAns = Optional.ofNullable(request.getParameter("q_" + q.getId())).orElse("");
            answers.put(String.valueOf(q.getId()), studentAns);

            // 保存到StudentAnswer
            StudentAnswer sa = new StudentAnswer();
            sa.setStudentId(user.getId());
            sa.setExamId(examId);
            sa.setQuestionId(q.getId());
            sa.setAnswer(studentAns);
            sa.setIsCorrect(0);
            sa.setScore(0);
            studentAnswerRepository.save(sa);
        }

        // 客观题评分并更新StudentAnswer
        int objective = 0;
        for (Question q : questions) {
            if ("essay".equals(q.getType())) continue;
            String studentAns = answers.getOrDefault(String.valueOf(q.getId()), "").trim();
            String correctAns = q.getAnswer() == null ? "" : q.getAnswer().trim();
            boolean correct = studentAns.equalsIgnoreCase(correctAns);
            if (correct) {
                objective += scoreMap.getOrDefault(q.getId(), 0);
            }
            StudentAnswer sa = studentAnswerRepository.findByStudentIdAndExamIdAndQuestionId(
                    user.getId(), examId, q.getId());
            if (sa != null) {
                sa.setIsCorrect(correct ? 1 : 0);
                sa.setScore(correct ? scoreMap.getOrDefault(q.getId(), 0) : 0);
                studentAnswerRepository.save(sa);
            }
        }

        boolean hasSubjective = questions.stream().anyMatch(q -> "essay".equals(q.getType()));

        // 创建或更新 ExamResult
        ExamResult result = new ExamResult();
        result.setStudentId(user.getId());
        result.setExamId(examId);
        result.setObjectiveScore(objective);
        result.setSubjectiveScore(0);
        result.setScore(objective);
        result.setGradeStatus(hasSubjective ? "待批阅" : "已批阅");
        result.setAnswersJson(objectMapper.writeValueAsString(answers));
        result.setSubmitTime(LocalDateTime.now());

        // 如果存在旧记录（允许重复考试），先删除旧记录
        examResultRepository.findByStudentIdAndExamId(user.getId(), examId)
                .ifPresent(old -> examResultRepository.delete(old));
        examResultRepository.save(result);

        return "redirect:/student/results";
    }
}