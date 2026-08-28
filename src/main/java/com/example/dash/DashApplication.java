package com.example.dash;

import com.example.dash.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DashApplication implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        // 🟢 บังคับให้ระบบของแอปพลิเคชันทั้งหมดใช้เวลาประเทศไทย
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bangkok"));
    }
    public static void main(String[] args) {
        SpringApplication.run(DashApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // สร้าง admin ครั้งแรกอัตโนมัติ
        // ถ้ามีอยู่แล้วจะไม่สร้างซ้ำ
        userService.register("admin", "admin1234", "ADMIN");
        System.out.println("Admin account ready");
    }
}