package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherController {
    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;

    public TeacherController(QuestionRepository questionRepository,
                             PaperRepository paperRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             ExamRepository examRepository,
                             ExamResultRepository examResultRepository) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
    }

    private boolean notTeacher(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || !"teacher".equals(user.getRole());
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        return "teacher_dashboard";
    }

    @GetMapping("/questions")
    public String questions(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("questions", questionRepository.findAll());
        return "question_list";
    }

    @GetMapping("/questions/add")
    public String addQuestionPage(HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        return "add_question";
    }

    @PostMapping("/questions/add")
    public String addQuestion(Question question, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        questionRepository.save(question);
        return "redirect:/teacher/questions";
    }

    @GetMapping("/papers/add")
    public String addPaperPage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("questions", questionRepository.findAll());
        return "add_paper";
    }

    @PostMapping("/papers/add")
    public String addPaper(Paper paper, @RequestParam(value = "questionIds", required = false) List<Long> questionIds,
                           @RequestParam(value = "sortOrders", required = false) List<Integer> sortOrders,
                           HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        paperRepository.save(paper);
        if (questionIds != null) {
            for (int i = 0; i < questionIds.size(); i++) {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(questionIds.get(i));
                pq.setSortOrder(sortOrders != null && i < sortOrders.size() ? sortOrders.get(i) : i);
                paperQuestionRepository.save(pq);
            }
        }
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/exams/add")
    public String addExamPage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("papers", paperRepository.findAll());
        return "add_exam";
    }

    @PostMapping("/exams/add")
    public String addExam(Exam exam,
                          @RequestParam("startTime") String startTimeStr,
                          @RequestParam("endTime") String endTimeStr,
                          HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        exam.setStatus("进行中");
        exam.setStartTime(LocalDateTime.parse(startTimeStr));
        exam.setEndTime(LocalDateTime.parse(endTimeStr));
        examRepository.save(exam);
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/results")
    public String results(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("results", examResultRepository.findAll());
        return "result";
    }

    // 在 TeacherController 中添加
    @PostMapping("/questions/delete/{id}")
    public String deleteQuestion(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (notTeacher(session)) return "redirect:/login";

        // 检查是否被试卷引用
        if (paperQuestionRepository.existsByQuestionId(id)) {
            redirectAttributes.addFlashAttribute("error", "该题目已被试卷引用，无法删除");
            return "redirect:/teacher/questions";
        }

        try {
            questionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "题目删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        return "redirect:/teacher/questions";
    }
}