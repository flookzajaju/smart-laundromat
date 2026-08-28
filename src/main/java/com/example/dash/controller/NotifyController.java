package com.example.dash.controller;

import com.example.dash.model.Machine;
import com.example.dash.repository.MachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    @Autowired
    private MachineRepository machineRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeToMachine(@RequestBody Map<String, String> payload) {

        String machineNumber = payload.get("machineNumber");
        String userId = payload.get("userId"); // 🟢 รับ UID ที่ส่งมาจาก LIFF

        // ค้นหาตู้ในฐานข้อมูล แล้วบันทึก UID ลูกค้าคนนี้เข้าไป
        Optional<Machine> opt = machineRepository.findByMachineNumber(machineNumber);
        if (opt.isPresent()) {
            Machine m = opt.get();
            m.setWaitingUserId(userId);
            machineRepository.save(m);
            System.out.println("✅ ลูกค้ารหัส " + userId + " จองคิวแจ้งเตือนเครื่อง " + machineNumber);
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}