package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserRepository userRepository;

    @Value("${app.admin.register-key:admin123}")
    private String adminRegisterKey;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String username, String password, HttpSession session, Model model) {
        User user = userRepository.findByUsernameAndPassword(username, password);
        if (user == null) {
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
        session.setAttribute("user", user);
        if ("admin".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        } else if ("teacher".equals(user.getRole())) {
            return "redirect:/teacher/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("classes", userRepository.findDistinctClassNames());
        return "register";
    }

    @PostMapping("/register")
    public String register(String username, String password, String realName, String role,
                           String className, String studentNo, String adminKey,
                           Model model, HttpSession session, HttpServletRequest request) {
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "用户名已存在，请更换");
            model.addAttribute("classes", userRepository.findDistinctClassNames());
            return "register";
        }
        if (password == null || password.length() < 3) {
            model.addAttribute("error", "密码长度至少3位");
            model.addAttribute("classes", userRepository.findDistinctClassNames());
            return "register";
        }
        // 管理员注册验证码校验
        if ("admin".equals(role)) {
            if (!adminRegisterKey.equals(adminKey)) {
                model.addAttribute("error", "管理员注册码错误");
                model.addAttribute("classes", userRepository.findDistinctClassNames());
                return "register";
            }
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setRealName(realName);
        newUser.setRole(role == null ? "student" : role);
        if ("student".equals(newUser.getRole())) {
            newUser.setClassName(className);
            newUser.setStudentNo(studentNo);
        }
        userRepository.save(newUser);
        session.setAttribute("user", newUser);
        if ("admin".equals(newUser.getRole())) {
            return "redirect:/admin/dashboard";
        } else if ("teacher".equals(newUser.getRole())) {
            return "redirect:/teacher/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }
}