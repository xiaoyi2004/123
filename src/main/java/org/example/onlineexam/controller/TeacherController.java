package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.example.onlineexam.service.QuestionImportService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/teacher")
public class TeacherController {
    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final QuestionImportService importService;

    public TeacherController(QuestionRepository questionRepository,
                             PaperRepository paperRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             ExamRepository examRepository,
                             ExamResultRepository examResultRepository,QuestionImportService importService) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.importService = importService;
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
    public String questions(@RequestParam(required = false) String type, Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        List<Question> questions;
        if (type != null && !type.isEmpty()) {
            questions = questionRepository.findByType(type);
        } else {
            questions = questionRepository.findAll();
        }
        model.addAttribute("questions", questions);
        model.addAttribute("selectedType", type);
        return "question_list";
    }

    @GetMapping("/questions/add")
    public String addQuestionPage(Model model) {
        model.addAttribute("question", new Question());
        return "add_question";
    }

    @PostMapping("/questions/add")
    public String addQuestion(@ModelAttribute Question question,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              HttpSession session) throws IOException {
        if (notTeacher(session)) return "redirect:/login";
        // 处理图片上传
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(uploadDir + fileName);
            imageFile.transferTo(dest);
            question.setImageUrl("/uploads/" + fileName);
        }
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

    // 新增：导入页面
    @GetMapping("/questions/import")
    public String importQuestionsPage() {
        return "import_questions";
    }

    // 新增：处理导入文件
    @PostMapping("/questions/import")
    public String importQuestions(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "请选择文件");
            return "redirect:/teacher/questions/import";
        }
        try {
            List<Question> questions = importService.parseQuestions(file);
            for (Question q : questions) {
                questionRepository.save(q);
            }
            ra.addFlashAttribute("success", "成功导入 " + questions.size() + " 道题目");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "解析失败：" + e.getMessage());
        }
        return "redirect:/teacher/questions";
    }

    @GetMapping("/papers")
    public String listPapers(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<Paper> papers = paperRepository.findAll();
        // 计算每个试卷的题目数量
        Map<Long, Integer> countMap = new HashMap<>();
        for (Paper p : papers) {
            int count = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(p.getId()).size();
            countMap.put(p.getId(), count);
        }
        model.addAttribute("papers", papers);
        model.addAttribute("paperQuestionCountMap", countMap);
        return "teacher_papers";
    }
    @PostMapping("/papers/delete/{id}")
    public String deletePaper(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notTeacher(session)) return "redirect:/login";
        // 检查是否被考试引用
        if (examRepository.existsByPaperId(id)) {
            ra.addFlashAttribute("error", "该试卷已被考试使用，无法删除");
            return "redirect:/teacher/papers";
        }
        // 删除试卷关联的题目关系
        paperQuestionRepository.deleteByPaperId(id);
        // 删除试卷
        paperRepository.deleteById(id);
        ra.addFlashAttribute("success", "试卷删除成功");
        return "redirect:/teacher/papers";
    }
}