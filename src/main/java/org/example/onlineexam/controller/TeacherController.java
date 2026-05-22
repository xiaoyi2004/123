package org.example.onlineexam.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.example.onlineexam.service.QuestionImportService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {
    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final UserRepository userRepository;
    private final QuestionImportService importService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeacherController(QuestionRepository questionRepository, PaperRepository paperRepository,
                             PaperQuestionRepository paperQuestionRepository, ExamRepository examRepository,
                             ExamResultRepository examResultRepository, UserRepository userRepository,
                             QuestionImportService importService) {
        this.questionRepository = questionRepository; this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository; this.examRepository = examRepository;
        this.examResultRepository = examResultRepository; this.userRepository = userRepository;
        this.importService = importService;
    }

    private boolean notTeacher(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || !"teacher".equals(user.getRole());
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("questionCount", questionRepository.count());
        model.addAttribute("paperCount", paperRepository.count());
        model.addAttribute("activeExamCount", examRepository.findByStatus("进行中").size());
        model.addAttribute("pendingGradingCount", examResultRepository.findByGradeStatus("待批阅").size());
        return "teacher_dashboard";
    }

    @GetMapping("/questions")
    public String questions(@RequestParam(required = false) String type,
                            @RequestParam(required = false) String subject,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Question> questionPage;
        boolean hasType = type != null && !type.isBlank();
        boolean hasSubject = subject != null && !subject.isBlank();
        if (hasType && hasSubject) questionPage = questionRepository.findByTypeAndSubject(type, subject, pageable);
        else if (hasType) questionPage = questionRepository.findByType(type, pageable);
        else if (hasSubject) questionPage = questionRepository.findBySubject(subject, pageable);
        else questionPage = questionRepository.findAll(pageable);
        model.addAttribute("questionPage", questionPage);
        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("subjects", questionRepository.findDistinctSubjects());
        return "question_list";
    }

    @GetMapping("/questions/add")
    public String addQuestionPage(Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("question", new Question());
        return "add_question";
    }

    @GetMapping("/questions/edit/{id}")
    public String editQuestionPage(@PathVariable Long id, Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        Question q = questionRepository.findById(id).orElse(null);
        if (q == null) return "redirect:/teacher/questions";
        model.addAttribute("question", q);
        return "add_question";
    }

    @PostMapping({"/questions/add", "/questions/edit/{id}"})
    public String saveQuestion(@PathVariable(required = false) Long id, @ModelAttribute Question question,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               HttpSession session) throws IOException {
        if (notTeacher(session)) return "redirect:/login";
        if (id != null) {
            Question old = questionRepository.findById(id).orElse(null);
            if (old != null && (imageFile == null || imageFile.isEmpty())) question.setImageUrl(old.getImageUrl());
            question.setId(id);
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir); if (!dir.exists()) dir.mkdirs();
            imageFile.transferTo(new File(uploadDir + fileName));
            question.setImageUrl("/uploads/" + fileName);
        }
        questionRepository.save(question);
        return "redirect:/teacher/questions";
    }

    @GetMapping("/questions/view/{id}")
    public String viewQuestion(@PathVariable Long id, Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("question", questionRepository.findById(id).orElse(null));
        return "question_view";
    }

    @PostMapping("/questions/delete/{id}")
    public String deleteQuestion(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notTeacher(session)) return "redirect:/login";
        if (paperQuestionRepository.existsByQuestionId(id)) { ra.addFlashAttribute("error", "该题目已被试卷引用，无法删除"); return "redirect:/teacher/questions"; }
        questionRepository.deleteById(id); ra.addFlashAttribute("success", "题目删除成功"); return "redirect:/teacher/questions";
    }

    @GetMapping("/questions/import") public String importQuestionsPage(HttpSession session) { return notTeacher(session) ? "redirect:/login" : "import_questions"; }
    @PostMapping("/questions/import")
    public String importQuestions(@RequestParam("file") MultipartFile file, RedirectAttributes ra, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        try { List<Question> qs = importService.parseQuestions(file); questionRepository.saveAll(qs); ra.addFlashAttribute("success", "成功导入 " + qs.size() + " 道题目"); }
        catch (Exception e) { ra.addFlashAttribute("error", "解析失败：" + e.getMessage()); }
        return "redirect:/teacher/questions";
    }

    @GetMapping("/papers")
    public String listPapers(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<Paper> papers = paperRepository.findAll(); Map<Long,Integer> countMap = new HashMap<>();
        for (Paper p : papers) countMap.put(p.getId(), paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(p.getId()).size());
        model.addAttribute("papers", papers); model.addAttribute("paperQuestionCountMap", countMap); return "teacher_papers";
    }

    @GetMapping("/papers/add")
    public String addPaperPage(@RequestParam(required = false) String type, @RequestParam(required = false) String subject,
                               @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        questions(type, subject, page, 20, model, session);
        model.addAttribute("paper", new Paper());
        return "add_paper";
    }

    @PostMapping("/papers/add")
    public String addPaper(Paper paper, @RequestParam(value="questionIds", required=false) List<Long> questionIds,
                           @RequestParam Map<String,String> params, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        paperRepository.save(paper);
        if (questionIds != null) {
            int order = 1;
            for (Long qid : questionIds) {
                PaperQuestion pq = new PaperQuestion(); pq.setPaperId(paper.getId()); pq.setQuestionId(qid);
                pq.setScore(Integer.parseInt(params.getOrDefault("score_" + qid, "0"))); pq.setSortOrder(order++);
                paperQuestionRepository.save(pq);
            }
        }
        return "redirect:/teacher/papers";
    }

    @PostMapping("/papers/delete/{id}")
    public String deletePaper(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notTeacher(session)) return "redirect:/login";
        if (examRepository.existsByPaperId(id)) { ra.addFlashAttribute("error", "该试卷已被考试使用，无法删除"); return "redirect:/teacher/papers"; }
        paperQuestionRepository.deleteByPaperId(id); paperRepository.deleteById(id); ra.addFlashAttribute("success", "试卷删除成功"); return "redirect:/teacher/papers";
    }

    @GetMapping("/exams/add")
    public String addExamPage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("papers", paperRepository.findAll()); model.addAttribute("classes", userRepository.findDistinctClassNames()); return "add_exam";
    }

    @PostMapping("/exams/add")
    public String addExam(Exam exam, @RequestParam("startTime") String startTimeStr, @RequestParam("endTime") String endTimeStr,
                          @RequestParam(value="classNamesList", required=false) List<String> classNamesList, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        paperRepository.findById(exam.getPaperId()).ifPresent(p -> exam.setSubject(p.getSubject()));
        exam.setClassNames(classNamesList == null ? "" : String.join(",", classNamesList));
        exam.setStatus("进行中"); exam.setStartTime(LocalDateTime.parse(startTimeStr)); exam.setEndTime(LocalDateTime.parse(endTimeStr));
        examRepository.save(exam); return "redirect:/teacher/dashboard";
    }

    @GetMapping("/results")
    public String results(@RequestParam(required=false) String className, HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<ExamResult> all = examResultRepository.findAll();
        Map<Long,User> students = userRepository.findByRole("student").stream().collect(Collectors.toMap(User::getId, u -> u));
        if (className != null && !className.isBlank()) all = all.stream().filter(r -> { User u = students.get(r.getStudentId()); return u != null && className.equals(u.getClassName()); }).collect(Collectors.toList());
        model.addAttribute("results", all); model.addAttribute("studentMap", students);
        model.addAttribute("examMap", examRepository.findAll().stream().collect(Collectors.toMap(Exam::getId, e -> e)));
        model.addAttribute("classes", userRepository.findDistinctClassNames()); model.addAttribute("selectedClass", className);
        return "result";
    }

    @GetMapping("/grading")
    public String gradingList(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<ExamResult> results = examResultRepository.findAll().stream().filter(r -> "待批阅".equals(r.getGradeStatus())).collect(Collectors.toList());
        model.addAttribute("results", results);
        model.addAttribute("studentMap", userRepository.findByRole("student").stream().collect(Collectors.toMap(User::getId, u -> u)));
        model.addAttribute("examMap", examRepository.findAll().stream().collect(Collectors.toMap(Exam::getId, e -> e)));
        return "grading_list";
    }

    @GetMapping("/grading/{resultId}")
    public String gradePage(@PathVariable Long resultId, HttpSession session, Model model) throws Exception {
        if (notTeacher(session)) return "redirect:/login";
        ExamResult result = examResultRepository.findById(resultId).orElse(null); if (result == null) return "redirect:/teacher/grading";
        Exam exam = examRepository.findById(result.getExamId()).orElse(null); if (exam == null) return "redirect:/teacher/grading";
        Map<String,String> answers = result.getAnswersJson()==null ? new HashMap<>() : objectMapper.readValue(result.getAnswersJson(), new TypeReference<>(){});
        Map<Long,Integer> scoreMap = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId()).stream().collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq.getScore()==null?0:pq.getScore()));
        List<Question> essays = paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId()).stream().map(pq -> questionRepository.findById(pq.getQuestionId()).orElse(null)).filter(Objects::nonNull).filter(q -> "essay".equals(q.getType())).collect(Collectors.toList());
        model.addAttribute("result", result); model.addAttribute("exam", exam); model.addAttribute("student", userRepository.findById(result.getStudentId()).orElse(null)); model.addAttribute("answers", answers); model.addAttribute("scoreMap", scoreMap); model.addAttribute("essays", essays);
        return "grade_subjective";
    }

    @PostMapping("/grading/{resultId}")
    public String saveGrade(@PathVariable Long resultId, @RequestParam Map<String,String> params, HttpSession session) throws Exception {
        if (notTeacher(session)) return "redirect:/login";
        ExamResult result = examResultRepository.findById(resultId).orElse(null); if (result == null) return "redirect:/teacher/grading";
        Exam exam = examRepository.findById(result.getExamId()).orElse(null); if (exam == null) return "redirect:/teacher/grading";
        int subjective = 0; Map<String,Integer> scores = new LinkedHashMap<>();
        for (PaperQuestion pq : paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId())) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null && "essay".equals(q.getType())) {
                int max = pq.getScore()==null?0:pq.getScore(); int s = Integer.parseInt(params.getOrDefault("score_" + q.getId(), "0"));
                s = Math.max(0, Math.min(max, s)); scores.put(String.valueOf(q.getId()), s); subjective += s;
            }
        }
        result.setSubjectiveScore(subjective); result.setScore((result.getObjectiveScore()==null?0:result.getObjectiveScore()) + subjective); result.setGradeStatus("已批阅"); result.setSubjectiveScoresJson(objectMapper.writeValueAsString(scores)); examResultRepository.save(result);
        return "redirect:/teacher/grading";
    }
}
