package com.example.dash.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "wash_cycles")
public class WashCycle {

    // ===== Properties (ประกาศตัวแปรทั้งหมดไว้ด้านบน) =====
    @Id
    private String id;

    private String machineId;               // อ้างอิงเครื่องไหน
    private String machineNumber;           // เลขเครื่อง
    private LocalDateTime startTime;        // เวลาเริ่มซัก
    private LocalDateTime endTime;          // เวลาเสร็จ

    private int durationMinutes;            // ระยะเวลารวมทั้งหมด (นาที)
    private int activeDurationMinutes;      // ระยะเวลาซักจริง ไม่รวมตอน Pause (นาที)

    private double avgAmplitude;            // ค่าสั่นเฉลี่ยตลอดรอบ
    private double maxAmplitude;            // ค่าสั่นสูงสุด
    private double rmsValue;                // ค่า RMS เฉลี่ยของรอบการซัก

    // ตัวแปรเก็บจุดพิกัดกราฟทั้งหมดในรอบซัก สำหรับเอาไปวาดกราฟ History
    private List<Double> vibrationHistory = new ArrayList<>();
    private List<Double> axHistory;
    // ===== Constructors =====
    public WashCycle() {
        // Default constructor สำหรับ MongoDB และ Jackson
    }

    public WashCycle(String machineId, String machineNumber) {
        this.machineId = machineId;
        this.machineNumber = machineNumber;
        this.startTime = LocalDateTime.now();
    }

    // ===== Business Logic =====
    // คำนวณระยะเวลาอัตโนมัติเมื่อจบรอบ
    public void finishCycle() {
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();
        }
    }

    // ===== Getters & Setters =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getMachineNumber() { return machineNumber; }
    public void setMachineNumber(String machineNumber) { this.machineNumber = machineNumber; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getActiveDurationMinutes() { return activeDurationMinutes; }
    public void setActiveDurationMinutes(int activeDurationMinutes) { this.activeDurationMinutes = activeDurationMinutes; }

    public double getAvgAmplitude() { return avgAmplitude; }
    public void setAvgAmplitude(double avgAmplitude) { this.avgAmplitude = avgAmplitude; }

    public double getMaxAmplitude() { return maxAmplitude; }
    public void setMaxAmplitude(double maxAmplitude) { this.maxAmplitude = maxAmplitude; }

    public double getRmsValue() { return rmsValue; }
    public void setRmsValue(double rmsValue) { this.rmsValue = rmsValue; }

    public List<Double> getVibrationHistory() { return vibrationHistory; }
    public void setVibrationHistory(List<Double> vibrationHistory) { this.vibrationHistory = vibrationHistory; }

    public List<Double> getAxHistory() {
        return axHistory;
    }
    public void setAxHistory(List<Double> axHistory) {
        this.axHistory = axHistory;
    }
}