package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.UserRepository;
import org.example.onlineexam.util.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.register-key:admin123}")
    private String adminRegisterKey;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String username, String password, HttpSession session, Model model) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            model.addAttribute("error", "用户名不存在");
            return "login";
        }

        // 处理 status 为 null（兼容旧数据）
        Integer status = user.getStatus();
        if (status == null || status == 0) {
            model.addAttribute("error", "用户已被禁用");
            return "login";
        }

        // 密码验证：先尝试 BCrypt，失败则尝试明文（兼容旧数据）
        boolean matched = false;
        String storedPwd = user.getPassword();
        if (storedPwd != null) {
            if (passwordEncoder.matches(password, storedPwd)) {
                matched = true;
            } else if (storedPwd.equals(password)) {
                // 明文匹配，说明密码未加密，立即加密并更新
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                matched = true;
            }
        }

        if (!matched) {
            model.addAttribute("error", "密码错误");
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
                           Model model, HttpSession session) {
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "用户名已存在");
            model.addAttribute("classes", userRepository.findDistinctClassNames());
            return "register";
        }
        if (password == null || password.length() < 3) {
            model.addAttribute("error", "密码长度至少3位");
            model.addAttribute("classes", userRepository.findDistinctClassNames());
            return "register";
        }
        if ("admin".equals(role)) {
            if (!adminRegisterKey.equals(adminKey)) {
                model.addAttribute("error", "管理员注册码错误");
                model.addAttribute("classes", userRepository.findDistinctClassNames());
                return "register";
            }
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRealName(realName);
        newUser.setRole(role == null ? "student" : role);
        newUser.setClassName(className);
        newUser.setStudentNo(studentNo); // 教师填工号，学生填学号
        newUser.setStatus(1);
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