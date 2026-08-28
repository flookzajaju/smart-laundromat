package com.example.dash.controller;

import com.example.dash.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // หน้า Login
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "username หรือ password ไม่ถูกต้อง");
        }
        return "login";
    }

    // หน้า Register
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           Model model) {
        boolean success = userService.register(username, password, "USER");
        if (!success) {
            model.addAttribute("error", "username นี้มีอยู่แล้ว");
            return "register";
        }
        return "redirect:/login?registered=true";
    }

    // Redirect หลัง login ตาม role
    @GetMapping("/redirect")
    public String redirect(Authentication auth) {
        if (auth.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/user/index";
    }
}