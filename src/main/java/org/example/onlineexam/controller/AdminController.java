package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.ExamRepository;
import org.example.onlineexam.repository.ExamResultRepository;
import org.example.onlineexam.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;

    public AdminController(UserRepository userRepository,
                           ExamRepository examRepository,
                           ExamResultRepository examResultRepository) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
    }

    private boolean notAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || !"admin".equals(user.getRole());
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (notAdmin(session)) return "redirect:/login";
        long userCount = userRepository.count();
        long examCount = examRepository.count();
        long resultCount = examResultRepository.count();
        model.addAttribute("userCount", userCount);
        model.addAttribute("examCount", examCount);
        model.addAttribute("resultCount", resultCount);
        return "admin_dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model, HttpSession session) {
        if (notAdmin(session)) return "redirect:/login";
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<User> userPage = userRepository.findAll(pageable);
        model.addAttribute("userPage", userPage);
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
        return "user_form";
    }

    @PostMapping("/users/save")
    public String saveUser(User user, @RequestParam(required = false) Long id,
                           HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) return "redirect:/login";
        if (id != null) user.setId(id);
        // 如果密码为空（编辑时未填），保留原密码
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            if (user.getId() != null) {
                User old = userRepository.findById(user.getId()).orElse(null);
                if (old != null) user.setPassword(old.getPassword());
            }
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
}