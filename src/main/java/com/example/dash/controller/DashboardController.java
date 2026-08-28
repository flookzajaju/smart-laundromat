package com.example.dash.controller;

import com.example.dash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    // หน้า user ทั่วไป
    @GetMapping("/user/index")
    public String userPage(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "user/index";
    }

    // หน้า admin
    @GetMapping("/admin/dashboard")
    public String adminPage(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("totalUsers",
                userRepository.count());
        return "admin/dashboard";
    }
}