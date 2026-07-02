package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.*;
import org.example.onlineexam.repository.*;
import org.example.onlineexam.util.PasswordEncoder;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ClazzRepository clazzRepository;
    private final AnnouncementRepository announcementRepository;
    private final OperationLogRepository operationLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           CourseRepository courseRepository,
                           ClazzRepository clazzRepository,
                           AnnouncementRepository announcementRepository,
                           OperationLogRepository operationLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.clazzRepository = clazzRepository;
        this.announcementRepository = announcementRepository;
        this.operationLogRepository = operationLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean notAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || !"admin".equals(user.getRole());
    }

    // ========== 仪表盘 ==========
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("courseCount", courseRepository.count());
        model.addAttribute("clazzCount", clazzRepository.count());
        model.addAttribute("announcementCount", announcementRepository.count());
        return "admin_dashboard";
    }

    // ========== 用户管理 ==========
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<User> userPage;
        if (keyword != null && !keyword.isBlank()) {
            // 简单搜索用户名或姓名
            userPage = userRepository.findByUsernameContainingOrRealNameContaining(keyword, keyword, pageable);
        } else if (role != null && !role.isBlank() && status != null) {
            userPage = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null && !role.isBlank()) {
            userPage = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            userPage = userRepository.findByStatus(status, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        return "user_list";
    }

    @GetMapping("/users/form")
    public String userForm(@RequestParam(required = false) Long id, Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) {
            model.addAttribute("user", userRepository.findById(id).orElse(new User()));
        } else {
            model.addAttribute("user", new User());
        }
        model.addAttribute("clazzes", clazzRepository.findAll());
        return "user_form";
    }

    @PostMapping("/users/save")
    public String saveUser(User user, @RequestParam(required = false) Long id,
                           HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) user.setId(id);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            if (user.getId() != null) {
                User old = userRepository.findById(user.getId()).orElse(null);
                if (old != null) user.setPassword(old.getPassword());
            }
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
        ra.addFlashAttribute("success", "保存成功");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        userRepository.deleteById(id);
        ra.addFlashAttribute("success", "删除成功");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/status/{id}")
    public String toggleUserStatus(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setStatus(user.getStatus() == 1 ? 0 : 1);
            userRepository.save(user);
            ra.addFlashAttribute("success", "状态已变更");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/resetPwd/{id}")
    public String resetPassword(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setPassword(passwordEncoder.encode("123456"));
            userRepository.save(user);
            ra.addFlashAttribute("success", "密码已重置为 123456");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/import")
    public String importStudents(@RequestParam("file") MultipartFile file,
                                 HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int success = 0, fail = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 跳过标题
                try {
                    String username = row.getCell(0).getStringCellValue();
                    String realName = row.getCell(1).getStringCellValue();
                    String password = row.getCell(2).getStringCellValue();
                    String className = row.getCell(3).getStringCellValue();
                    String phone = row.getCell(4).getStringCellValue();
                    String email = row.getCell(5).getStringCellValue();

                    if (userRepository.findByUsername(username) != null) {
                        fail++;
                        continue;
                    }
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRealName(realName);
                    user.setRole("student");
                    user.setClassName(className);
                    user.setPhone(phone);
                    user.setEmail(email);
                    user.setStatus(1);
                    userRepository.save(user);
                    success++;
                } catch (Exception e) {
                    fail++;
                }
            }
            ra.addFlashAttribute("success", "导入成功 " + success + " 条，失败 " + fail + " 条");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "导入失败：" + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ========== 课程管理 ==========
    @GetMapping("/courses")
    public String listCourses(Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("courses", courseRepository.findAll());
        return "course_list";
    }

    @GetMapping("/courses/form")
    public String courseForm(@RequestParam(required = false) Long id, Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) {
            model.addAttribute("course", courseRepository.findById(id).orElse(new Course()));
        } else {
            model.addAttribute("course", new Course());
        }
        return "course_form";
    }

    @PostMapping("/courses/save")
    public String saveCourse(Course course, @RequestParam(required = false) Long id,
                             HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) course.setId(id);
        courseRepository.save(course);
        ra.addFlashAttribute("success", "保存成功");
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        courseRepository.deleteById(id);
        ra.addFlashAttribute("success", "删除成功");
        return "redirect:/admin/courses";
    }

    // ========== 班级管理 ==========
    @GetMapping("/clazzes")
    public String listClazzes(Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("clazzes", clazzRepository.findAll());
        model.addAttribute("courses", courseRepository.findAll());
        return "clazz_list";
    }

    @GetMapping("/clazzes/form")
    public String clazzForm(@RequestParam(required = false) Long id, Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) {
            model.addAttribute("clazz", clazzRepository.findById(id).orElse(new Clazz()));
        } else {
            model.addAttribute("clazz", new Clazz());
        }
        model.addAttribute("courses", courseRepository.findAll());
        return "clazz_form";
    }

    @PostMapping("/clazzes/save")
    public String saveClazz(Clazz clazz, @RequestParam(required = false) Long id,
                            HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) clazz.setId(id);
        clazzRepository.save(clazz);
        ra.addFlashAttribute("success", "保存成功");
        return "redirect:/admin/clazzes";
    }

    @PostMapping("/clazzes/delete/{id}")
    public String deleteClazz(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        clazzRepository.deleteById(id);
        ra.addFlashAttribute("success", "删除成功");
        return "redirect:/admin/clazzes";
    }

    // ========== 公告管理 ==========
    @GetMapping("/announcements")
    public String listAnnouncements(Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        model.addAttribute("announcements", announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "announcement_list";
    }

    @GetMapping("/announcements/form")
    public String announcementForm(@RequestParam(required = false) Long id, Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) {
            model.addAttribute("announcement", announcementRepository.findById(id).orElse(new Announcement()));
        } else {
            model.addAttribute("announcement", new Announcement());
        }
        return "announcement_form";
    }

    @PostMapping("/announcements/save")
    public String saveAnnouncement(Announcement announcement, @RequestParam(required = false) Long id,
                                   HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        User user = (User) session.getAttribute("user");
        if (id != null) announcement.setId(id);
        announcement.setPublisherId(user.getId());
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementRepository.save(announcement);
        ra.addFlashAttribute("success", "保存成功");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/delete/{id}")
    public String deleteAnnouncement(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        announcementRepository.deleteById(id);
        ra.addFlashAttribute("success", "删除成功");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/publish/{id}")
    public String publishAnnouncement(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        Announcement a = announcementRepository.findById(id).orElse(null);
        if (a != null) {
            a.setStatus(1);
            announcementRepository.save(a);
            ra.addFlashAttribute("success", "已发布");
        }
        return "redirect:/admin/announcements";
    }

    // ========== 操作日志 ==========
    @GetMapping("/logs")
    public String listLogs(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String module,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logPage;
        // 简版查询，不实现日期范围，请按需扩展
        if (username != null && !username.isBlank()) {
            logPage = operationLogRepository.findByUsernameContaining(username, pageable);
        } else if (module != null && !module.isBlank()) {
            logPage = operationLogRepository.findByModule(module, pageable);
        } else {
            logPage = operationLogRepository.findAll(pageable);
        }
        model.addAttribute("logPage", logPage);
        model.addAttribute("username", username);
        model.addAttribute("module", module);
        return "operation_log_list";
    }
}