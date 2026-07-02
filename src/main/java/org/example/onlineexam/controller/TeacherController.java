package org.example.onlineexam.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.example.onlineexam.service.*;
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
    private final ClazzRepository clazzRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final CourseRepository courseRepository;
    private final QuestionImportService importService;
    private final StatisticsService statisticsService;
    private final ExcelExportService excelExportService;
    private final AutoPaperService autoPaperService;
    private final KnowledgeTreeService knowledgeTreeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeacherController(QuestionRepository questionRepository,
                             PaperRepository paperRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             ExamRepository examRepository,
                             ExamResultRepository examResultRepository,
                             UserRepository userRepository,
                             ClazzRepository clazzRepository,
                             KnowledgePointRepository knowledgePointRepository,
                             CourseRepository courseRepository,
                             QuestionImportService importService,
                             StatisticsService statisticsService,
                             ExcelExportService excelExportService,
                             AutoPaperService autoPaperService,
                             KnowledgeTreeService knowledgeTreeService) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.userRepository = userRepository;
        this.clazzRepository = clazzRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.courseRepository = courseRepository;
        this.importService = importService;
        this.statisticsService = statisticsService;
        this.excelExportService = excelExportService;
        this.autoPaperService = autoPaperService;
        this.knowledgeTreeService = knowledgeTreeService;
    }

    private boolean notTeacher(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || (!"teacher".equals(user.getRole()) && !"admin".equals(user.getRole()));
    }

    // ==================== 仪表盘 ====================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        return notTeacher(session) ? "redirect:/login" : "teacher_dashboard";
    }

    // ==================== 题库管理 ====================
    @GetMapping("/questions")
    public String questions(@RequestParam(required = false) String type,
                            @RequestParam(required = false) String subject,
                            @RequestParam(required = false) Integer difficulty,
                            @RequestParam(required = false) String knowledgePoint,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Question> questionPage;
        if (type != null && !type.isBlank() && subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findByTypeAndSubject(type, subject, pageable);
        } else if (type != null && !type.isBlank()) {
            questionPage = questionRepository.findByType(type, pageable);
        } else if (subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findBySubject(subject, pageable);
        } else {
            questionPage = questionRepository.findAll(pageable);
        }
        model.addAttribute("questionPage", questionPage);
        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedSubject", subject);
        // 所有科目列表（用于下拉筛选）
        model.addAttribute("subjects", questionRepository.findDistinctSubjects());
        model.addAttribute("knowledgePoints", knowledgePointRepository.findAll());
        return "question_list";
    }

    @GetMapping("/questions/edit/{id}")
    public String editQuestionPage(@PathVariable Long id,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        Question q = questionRepository.findById(id).orElse(null);
        if (q == null) return "redirect:/teacher/questions";
        model.addAttribute("question", q);
        // 传递所有课程
        model.addAttribute("courseList", courseRepository.findAll());
        model.addAttribute("knowledgePoints", knowledgePointRepository.findAll());
        model.addAttribute("page", page);
        return "add_question";
    }

    @GetMapping("/questions/add")
    public String addQuestionPage(Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("question", new Question());
        model.addAttribute("courseList", courseRepository.findAll()); // 新增也需要
        model.addAttribute("knowledgePoints", knowledgePointRepository.findAll());
        model.addAttribute("page", 0);
        return "add_question";
    }

    @PostMapping({"/questions/add", "/questions/edit/{id}"})
    public String saveQuestion(@PathVariable(required = false) Long id,
                               @ModelAttribute Question question,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               @RequestParam(defaultValue = "0") int page,
                               HttpSession session) throws IOException {

        // 权限校验
        if (notTeacher(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");

        // 如果是修改操作，保留原有的图片URL（如果未上传新图片）
        if (id != null) {
            Question old = questionRepository.findById(id).orElse(null);
            if (old != null && (imageFile == null || imageFile.isEmpty())) {
                question.setImageUrl(old.getImageUrl());
            }
            question.setId(id);
        }

        // 设置创建人
        question.setCreatorId(user.getId());

        // 处理图片上传
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            imageFile.transferTo(new File(uploadDir + fileName));
            question.setImageUrl("/uploads/" + fileName);
        }

        // 如果选择了知识点，同步存储知识点名称（冗余字段）
        if (question.getKnowledgePointId() != null) {
            knowledgePointRepository.findById(question.getKnowledgePointId())
                    .ifPresent(kp -> question.setKnowledgePoint(kp.getName()));
        }

        // ★★★ 关键：保存题目对象，Spring 会自动绑定表单中 name="subject" 的值 ★★★
        // 确保前端表单中 <select name="subject"> 或 <input name="subject"> 存在且值正确
        questionRepository.save(question);

        // 重定向回列表页，并携带页码
        return "redirect:/teacher/questions?page=" + page;
    }

    @GetMapping("/questions/view/{id}")
    public String viewQuestion(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int page,
                               Model model, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        Question q = questionRepository.findById(id).orElse(null);
        model.addAttribute("question", q);
        model.addAttribute("page", page);
        return "question_view";
    }

    @PostMapping("/questions/delete/{id}")
    public String deleteQuestion(@PathVariable Long id,
                                 @RequestParam(defaultValue = "0") int page,
                                 HttpSession session, RedirectAttributes ra) {
        if (notTeacher(session)) return "redirect:/login";
        if (paperQuestionRepository.existsByQuestionId(id)) {
            ra.addFlashAttribute("error", "该题目已被试卷引用，无法删除");
            return "redirect:/teacher/questions?page=" + page;
        }
        questionRepository.deleteById(id);
        ra.addFlashAttribute("success", "题目删除成功");
        return "redirect:/teacher/questions?page=" + page;
    }

    @PostMapping("/questions/batch-delete")
    @ResponseBody
    public Map<String, Object> batchDeleteQuestions(@RequestParam("ids") List<Long> ids, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (notTeacher(session)) {
            result.put("success", false);
            result.put("message", "无权限");
            return result;
        }
        List<Long> deletedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        for (Long id : ids) {
            if (paperQuestionRepository.existsByQuestionId(id)) {
                failedIds.add(id);
            } else {
                questionRepository.deleteById(id);
                deletedIds.add(id);
            }
        }
        result.put("success", true);
        result.put("deletedCount", deletedIds.size());
        result.put("failedCount", failedIds.size());
        result.put("failedIds", failedIds);
        return result;
    }

    @GetMapping("/questions/import")
    public String importQuestionsPage(HttpSession session) {
        return notTeacher(session) ? "redirect:/login" : "import_questions";
    }

    @PostMapping("/questions/import")
    public String importQuestions(@RequestParam("file") MultipartFile file,
                                  RedirectAttributes ra, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        try {
            List<Question> qs = importService.parseQuestions(file);
            User user = (User) session.getAttribute("user");
            qs.forEach(q -> q.setCreatorId(user.getId()));
            // 批量保存前，可先验证、清理
            questionRepository.saveAll(qs);
            ra.addFlashAttribute("success", "成功导入 " + qs.size() + " 道题目");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "解析失败：" + e.getMessage());
        }
        return "redirect:/teacher/questions";
    }

    // ==================== 试卷管理 ====================
    @GetMapping("/papers")
    public String listPapers(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<Paper> papers = paperRepository.findAll();
        Map<Long, Integer> countMap = new HashMap<>();
        for (Paper p : papers) {
            countMap.put(p.getId(), paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(p.getId()).size());
        }
        model.addAttribute("papers", papers);
        model.addAttribute("paperQuestionCountMap", countMap);
        return "teacher_papers";
    }

    @GetMapping("/papers/add")
    public String addPaperPage(@RequestParam(required = false) String type,
                               @RequestParam(required = false) String subject,
                               @RequestParam(defaultValue = "0") int page,
                               HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(page, 20, Sort.by("id"));
        Page<Question> questionPage;
        if (type != null && !type.isBlank() && subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findByTypeAndSubject(type, subject, pageable);
        } else if (type != null && !type.isBlank()) {
            questionPage = questionRepository.findByType(type, pageable);
        } else if (subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findBySubject(subject, pageable);
        } else {
            questionPage = questionRepository.findAll(pageable);
        }
        model.addAttribute("questionPage", questionPage);
        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedSubject", subject);
        // 传递所有不重复科目（用于下拉）
        model.addAttribute("subjects", questionRepository.findDistinctSubjects());
        // 传递所有题型选项（用于下拉）
        model.addAttribute("questionTypes", Arrays.asList(
                "single", "multiple_choice", "judge", "fill", "essay", "analysis", "programming"
        ));
        model.addAttribute("paper", new Paper());
        model.addAttribute("knowledgePoints", knowledgePointRepository.findAll());
        return "add_paper";
    }

    @PostMapping("/papers/add")
    public String addPaper(Paper paper,
                           @RequestParam(value = "questionIds", required = false) List<Long> questionIds,
                           @RequestParam Map<String, String> params,
                           HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        User user = (User) session.getAttribute("user");
        paper.setCreatorId(user.getId());
        paperRepository.save(paper);

        int totalScore = 0;
        if (questionIds != null) {
            int order = 1;
            for (Long qid : questionIds) {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(qid);
                int score = Integer.parseInt(params.getOrDefault("score_" + qid, "0"));
                pq.setScore(score);
                pq.setSortOrder(order++);
                paperQuestionRepository.save(pq);
                totalScore += score;
            }
        }
        paper.setTotalScore(totalScore);
        paperRepository.save(paper);
        return "redirect:/teacher/papers";
    }

    @PostMapping("/papers/delete/{id}")
    public String deletePaper(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notTeacher(session)) return "redirect:/login";
        if (examRepository.existsByPaperId(id)) {
            ra.addFlashAttribute("error", "该试卷已被考试使用，无法删除");
            return "redirect:/teacher/papers";
        }
        paperQuestionRepository.deleteByPaperId(id);
        paperRepository.deleteById(id);
        ra.addFlashAttribute("success", "试卷删除成功");
        return "redirect:/teacher/papers";
    }

    // ==================== 智能组卷 ====================
    @GetMapping("/papers/auto")
    public String autoPaperPage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("knowledgePoints", knowledgePointRepository.findAll());
        model.addAttribute("questionTypes", Arrays.asList(
                "single", "multiple_choice", "judge", "fill", "essay", "analysis", "programming"
        ));
        return "auto_paper";
    }

    @PostMapping("/papers/auto/generate")
    @ResponseBody
    public Map<String, Object> generatePaper(@RequestBody Map<String, Object> config, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (notTeacher(session)) {
            result.put("success", false);
            result.put("message", "无权限");
            return result;
        }
        try {
            Paper paper = autoPaperService.generatePaper(config);
            result.put("success", true);
            result.put("paperId", paper.getId());
            result.put("message", "组卷成功，试卷ID：" + paper.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 知识库管理 ====================
    @GetMapping("/knowledge")
    public String knowledgeManage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("allNodes", knowledgePointRepository.findAll());
        model.addAttribute("tree", knowledgeTreeService.getTreeByCourse(null));
        model.addAttribute("courses", courseRepository.findAll());
        return "knowledge_manage";
    }

    @PostMapping("/knowledge/save")
    public String saveKnowledge(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String courseId,
                                @RequestParam(required = false) String parentId,
                                HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        KnowledgePoint kp = new KnowledgePoint();
        kp.setName(name);
        kp.setDescription(description);
        if (courseId != null && !courseId.isBlank()) {
            kp.setCourseId(Long.parseLong(courseId));
        }
        if (parentId != null && !parentId.isBlank()) {
            kp.setParentId(Long.parseLong(parentId));
        }
        if (kp.getParentId() != null && kp.getParentId() > 0) {
            knowledgePointRepository.findById(kp.getParentId()).ifPresent(p -> {
                kp.setLevel(p.getLevel() != null ? p.getLevel() + 1 : 1);
            });
        } else {
            kp.setLevel(0);
        }
        kp.setCreatedAt(LocalDateTime.now());
        kp.setUpdatedAt(LocalDateTime.now());
        knowledgePointRepository.save(kp);
        return "redirect:/teacher/knowledge";
    }

    @GetMapping("/knowledge/delete/{id}")
    public String deleteKnowledge(@PathVariable Long id, HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        knowledgePointRepository.deleteById(id);
        return "redirect:/teacher/knowledge";
    }

    // ==================== 考试发布 ====================
    @GetMapping("/exams/add")
    public String addExamPage(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        model.addAttribute("papers", paperRepository.findAll());
        List<String> classNames = clazzRepository.findAll().stream()
                .map(Clazz::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (classNames.isEmpty()) {
            classNames = userRepository.findDistinctClassNames();
        }
        model.addAttribute("classes", classNames);
        return "add_exam";
    }

    @PostMapping("/exams/add")
    public String addExam(Exam exam,
                          @RequestParam("startTime") String startTimeStr,
                          @RequestParam("endTime") String endTimeStr,
                          @RequestParam(value = "classNamesList", required = false) List<String> classNamesList,
                          @RequestParam(defaultValue = "0") Integer allowRepeat,
                          @RequestParam(defaultValue = "0") Integer shuffleQuestions,
                          @RequestParam(defaultValue = "1") Integer showScore,
                          @RequestParam(defaultValue = "1") Integer showAnalysis,
                          HttpSession session) {
        if (notTeacher(session)) return "redirect:/login";
        User user = (User) session.getAttribute("user");
        exam.setCreatorId(user.getId());
        exam.setClassNames(classNamesList == null ? "" : String.join(",", classNamesList));
        exam.setStatus("进行中");
        exam.setStartTime(LocalDateTime.parse(startTimeStr));
        exam.setEndTime(LocalDateTime.parse(endTimeStr));
        exam.setAllowRepeat(allowRepeat);
        exam.setShuffleQuestions(shuffleQuestions);
        exam.setShowScore(showScore);
        exam.setShowAnalysis(showAnalysis);
        paperRepository.findById(exam.getPaperId()).ifPresent(p -> exam.setSubject(p.getSubject()));
        examRepository.save(exam);
        return "redirect:/teacher/dashboard";
    }

    // ==================== 成绩管理 ====================
    @GetMapping("/results")
    public String results(@RequestParam(required = false) Long examId,
                          @RequestParam(required = false) String className,
                          HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";

        List<Exam> exams = examRepository.findAll();
        model.addAttribute("exams", exams);
        model.addAttribute("selectedExamId", examId);

        List<String> classNames = clazzRepository.findAll().stream()
                .map(Clazz::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (classNames.isEmpty()) {
            classNames = userRepository.findDistinctClassNames();
        }
        model.addAttribute("classes", classNames);
        model.addAttribute("selectedClass", className);

        List<ExamResult> results;
        if (examId != null) {
            results = examResultRepository.findByExamId(examId);
        } else {
            results = examResultRepository.findAll();
        }

        if (className != null && !className.isBlank()) {
            Map<Long, User> students = userRepository.findByRole("student")
                    .stream().collect(Collectors.toMap(User::getId, u -> u));
            results = results.stream().filter(r -> {
                User u = students.get(r.getStudentId());
                return u != null && className.equals(u.getClassName());
            }).collect(Collectors.toList());
        }

        model.addAttribute("results", results);
        model.addAttribute("studentMap", userRepository.findByRole("student")
                .stream().collect(Collectors.toMap(User::getId, u -> u)));
        model.addAttribute("examMap", exams.stream().collect(Collectors.toMap(Exam::getId, e -> e)));

        if (!results.isEmpty() && examId != null) {
            model.addAttribute("statistics", statisticsService.getExamStatistics(examId));
        } else if (!results.isEmpty()) {
            Long firstExamId = results.get(0).getExamId();
            model.addAttribute("statistics", statisticsService.getExamStatistics(firstExamId));
        }
        return "result";
    }

    @GetMapping("/results/export/{examId}")
    public void exportResults(@PathVariable Long examId,
                              HttpServletResponse response,
                              HttpSession session) throws IOException {
        if (notTeacher(session)) return;
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return;
        excelExportService.exportExamResults(examId, exam, response);
    }

    @GetMapping("/results/statistics/{examId}")
    @ResponseBody
    public Map<String, Object> getStatistics(@PathVariable Long examId, HttpSession session) {
        if (notTeacher(session)) return Map.of();
        return statisticsService.getExamStatistics(examId);
    }

    // ==================== 主观题批阅 ====================
    @GetMapping("/grading")
    public String gradingList(HttpSession session, Model model) {
        if (notTeacher(session)) return "redirect:/login";
        List<ExamResult> results = examResultRepository.findAll().stream()
                .filter(r -> "待批阅".equals(r.getGradeStatus()))
                .collect(Collectors.toList());
        model.addAttribute("results", results);
        model.addAttribute("studentMap", userRepository.findByRole("student")
                .stream().collect(Collectors.toMap(User::getId, u -> u)));
        model.addAttribute("examMap", examRepository.findAll().stream()
                .collect(Collectors.toMap(Exam::getId, e -> e)));
        return "grading_list";
    }

    @GetMapping("/grading/{resultId}")
    public String gradePage(@PathVariable Long resultId, HttpSession session, Model model) throws Exception {
        if (notTeacher(session)) return "redirect:/login";
        ExamResult result = examResultRepository.findById(resultId).orElse(null);
        if (result == null) return "redirect:/teacher/grading";
        Exam exam = examRepository.findById(result.getExamId()).orElse(null);
        if (exam == null) return "redirect:/teacher/grading";

        Map<String, String> answers = result.getAnswersJson() == null ?
                new HashMap<>() :
                objectMapper.readValue(result.getAnswersJson(), new TypeReference<>() {});

        Map<Long, Integer> scoreMap = paperQuestionRepository
                .findByPaperIdOrderBySortOrderAsc(exam.getPaperId())
                .stream().collect(Collectors.toMap(PaperQuestion::getQuestionId,
                        pq -> pq.getScore() == null ? 0 : pq.getScore()));

        List<Question> essays = paperQuestionRepository
                .findByPaperIdOrderBySortOrderAsc(exam.getPaperId())
                .stream()
                .map(pq -> questionRepository.findById(pq.getQuestionId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(q -> "essay".equals(q.getType()) || "analysis".equals(q.getType()) || "programming".equals(q.getType()))
                .collect(Collectors.toList());

        model.addAttribute("result", result);
        model.addAttribute("exam", exam);
        model.addAttribute("student", userRepository.findById(result.getStudentId()).orElse(null));
        model.addAttribute("answers", answers);
        model.addAttribute("scoreMap", scoreMap);
        model.addAttribute("essays", essays);
        return "grade_subjective";
    }

    @PostMapping("/grading/{resultId}")
    public String saveGrade(@PathVariable Long resultId,
                            @RequestParam Map<String, String> params,
                            HttpSession session) throws Exception {
        if (notTeacher(session)) return "redirect:/login";
        ExamResult result = examResultRepository.findById(resultId).orElse(null);
        if (result == null) return "redirect:/teacher/grading";
        Exam exam = examRepository.findById(result.getExamId()).orElse(null);
        if (exam == null) return "redirect:/teacher/grading";

        int subjective = 0;
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (PaperQuestion pq : paperQuestionRepository.findByPaperIdOrderBySortOrderAsc(exam.getPaperId())) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null && ("essay".equals(q.getType()) || "analysis".equals(q.getType()) || "programming".equals(q.getType()))) {
                int max = pq.getScore() == null ? 0 : pq.getScore();
                int s = Integer.parseInt(params.getOrDefault("score_" + q.getId(), "0"));
                s = Math.max(0, Math.min(max, s));
                scores.put(String.valueOf(q.getId()), s);
                subjective += s;
            }
        }
        result.setSubjectiveScore(subjective);
        result.setScore((result.getObjectiveScore() == null ? 0 : result.getObjectiveScore()) + subjective);
        result.setGradeStatus("已批阅");
        result.setSubjectiveScoresJson(objectMapper.writeValueAsString(scores));
        examResultRepository.save(result);
        return "redirect:/teacher/grading";
    }
}