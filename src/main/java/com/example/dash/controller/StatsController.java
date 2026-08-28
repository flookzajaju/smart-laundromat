package com.example.dash.controller;

import com.example.dash.model.WashCycle;
import com.example.dash.repository.WashCycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private WashCycleRepository washCycleRepository;

    // ค่าเฉลี่ยเวลาต่อรอบแยกตามเครื่อง
    @GetMapping("/avg")
    public List<Map<String, Object>> getAvgByMachine() {
        List<WashCycle> all = washCycleRepository.findAll();

        // Group by machineNumber แล้วคำนวณเฉลี่ย
        Map<String, List<WashCycle>> grouped = all.stream()
                .filter(c -> c.getDurationMinutes() > 0)
                .collect(Collectors.groupingBy(
                        WashCycle::getMachineNumber));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((machine, cycles) -> {
            double avgMin = cycles.stream()
                    .mapToInt(WashCycle::getDurationMinutes)
                    .average().orElse(0);
            double avgAmp = cycles.stream()
                    .mapToDouble(WashCycle::getAvgAmplitude)
                    .average().orElse(0);
            int totalCycles = cycles.size();

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("machineNumber", machine);
            stat.put("totalCycles", totalCycles);
            stat.put("avgMinutes",
                    Math.round(avgMin * 10.0) / 10.0);
            stat.put("avgAmplitude",
                    Math.round(avgAmp * 1000.0) / 1000.0);
            stat.put("minMinutes", cycles.stream()
                    .mapToInt(WashCycle::getDurationMinutes)
                    .min().orElse(0));
            stat.put("maxMinutes", cycles.stream()
                    .mapToInt(WashCycle::getDurationMinutes)
                    .max().orElse(0));
            result.add(stat);
        });

        return result;
    }

    // สถิติรายวัน 7 วันล่าสุด (แก้ตรงการ group ให้ครอบคลุม)
    @GetMapping("/cycles")
    public List<Map<String, Object>> getDailyCycles() {
        // ใช้ 00:00:00 ของเมื่อ 7 วันก่อน
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7).withHour(0).withMinute(0);
        List<WashCycle> recent = washCycleRepository.findByStartTimeAfter(sevenDaysAgo);

        // Group by วันที่
        Map<LocalDate, List<WashCycle>> byDay = recent.stream()
                .filter(c -> c.getStartTime() != null)
                .collect(Collectors.groupingBy(c -> c.getStartTime().toLocalDate()));

        List<Map<String, Object>> result = new ArrayList<>();
        // ลูปให้ครบ 7 วัน
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<WashCycle> dayCycles = byDay.getOrDefault(date, Collections.emptyList());

            double avgMin = dayCycles.stream()
                    .mapToInt(WashCycle::getDurationMinutes)
                    .average().orElse(0.0);

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString()); // หน้าเว็บต้องรอรับคีย์นี้
            day.put("count", dayCycles.size());
            day.put("avgMinutes", Math.round(avgMin * 10.0) / 10.0);
            result.add(day);
        }
        return result;
    }

    // ประวัติรอบซักของเครื่องนั้นๆ
    @GetMapping("/cycles/{machineNumber}")
    public List<Map<String, Object>> getCyclesByMachine(
            @PathVariable String machineNumber) {

        List<WashCycle> cycles = washCycleRepository
                .findByMachineNumberOrderByStartTimeDesc(machineNumber);

        return cycles.stream().limit(20).map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("startTime", c.getStartTime());
            m.put("endTime", c.getEndTime());
            m.put("durationMinutes", c.getDurationMinutes());
            m.put("avgAmplitude", c.getAvgAmplitude());
            m.put("maxAmplitude", c.getMaxAmplitude());
            return m;
        }).collect(Collectors.toList());
    }
}