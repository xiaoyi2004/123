package org.example.onlineexam.controller;

import jakarta.servlet.http.HttpSession;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 原有 login 页面
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
        if ("teacher".equals(user.getRole())) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/student/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ========== 新增注册功能 ==========
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(String username, String password, String realName, String role, Model model, HttpSession session) {
        // 校验用户名是否已存在
        User existing = userRepository.findByUsernameAndPassword(username, password); // 这里密码传任意值其实不严谨，最好有一个按用户名查询的方法
        // 更好的做法：单独写一个方法 findByUsername，这里为了简单，直接用 JPA 方法名规范
        // 但因为 UserRepository 中没有 findByUsername，我们需要临时添加，或者使用 JPQL
        // 我们将在 UserRepository 中添加方法：User findByUsername(String username);
        // 假设已添加，现使用：
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "用户名已存在，请更换");
            return "register";
        }
        // 简单密码长度校验
        if (password == null || password.length() < 3) {
            model.addAttribute("error", "密码长度至少3位");
            return "register";
        }
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setRealName(realName);
        newUser.setRole(role);  // teacher / student
        userRepository.save(newUser);

        // 注册成功后自动登录
        session.setAttribute("user", newUser);
        if ("teacher".equals(role)) {
            return "redirect:/teacher/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }
}