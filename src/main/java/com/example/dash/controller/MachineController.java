package com.example.dash.controller;

import com.example.dash.model.Machine;
import com.example.dash.model.WashCycle;
import com.example.dash.repository.MachineRepository;
import com.example.dash.repository.WashCycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MachineController {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private WashCycleRepository washCycleRepository;

    @GetMapping("/machines")
    public List<Map<String, Object>> getAllMachines() {
        List<Machine> machines = machineRepository
                .findAllByOrderByMachineNumberAsc();

        return machines.stream().map(m -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", m.getId());
            result.put("machineNumber", m.getMachineNumber());
            result.put("status", m.getStatus());
            result.put("amplitude", m.getAmplitude());
            result.put("doorOpen", m.isDoorOpen());
            result.put("lastUpdated", m.getLastUpdated());

            // 🔥 แก้ไขแล้ว: คำนวณเวลาซักรวม (ให้เวลาเดินหน้าเสมอ ไม่หักช่วง Pause ออก)
            if (m.getCycleStartTime() != null &&
                    ("RUNNING".equals(m.getStatus()) ||
                            "PAUSED".equals(m.getStatus()))) {

                // คำนวณเวลาที่ผ่านไปทั้งหมด (วินาทีปัจจุบัน ลบ วินาทีที่เริ่มซัก)
                long totalSec = java.time.Duration
                        .between(m.getCycleStartTime(), LocalDateTime.now())
                        .getSeconds();

                // บังคับส่งเวลาที่เดินหน้าตลอดไปให้หน้าเว็บ
                result.put("elapsedSeconds", totalSec);

                // ประมาณเวลาที่เหลือจาก Machine Profile
                // ดึงค่าเฉลี่ยจาก WashCycle ที่ผ่านมา
                double avgMin = getAvgCycleMinutes(m.getMachineNumber());
                if (avgMin > 0) {
                    // เอาเวลาเฉลี่ยมาหักลบกับเวลาที่เดินไปแล้วทั้งหมด
                    long remainSec = (long)(avgMin * 60) - totalSec;
                    result.put("estimatedRemainSeconds", Math.max(remainSec, 0));
                } else {
                    result.put("estimatedRemainSeconds", null);
                }
            } else {
                result.put("elapsedSeconds", null);
                result.put("estimatedRemainSeconds", null);
            }

            return result;
        }).collect(Collectors.toList());
    }

    // หาค่าเฉลี่ยรอบซักจาก WashCycle
    private double getAvgCycleMinutes(String machineNumber) {
        List<WashCycle> cycles = washCycleRepository
                .findByMachineNumberOrderByStartTimeDesc(machineNumber);
        if (cycles.isEmpty()) return 0;
        return cycles.stream()
                .mapToInt(WashCycle::getDurationMinutes)
                .average()
                .orElse(0);
    }
}