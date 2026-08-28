package com.example.dash.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient; // เพิ่มบรรทัดนี้เพื่อใช้งาน @Transient
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "machines")
public class Machine {

    @Id
    private String id;

    private String machineNumber;
    private String status;           // IDLE, RUNNING, PAUSED, DONE
    private boolean doorOpen;
    private double amplitude;
    private LocalDateTime lastUpdated;
    private LocalDateTime cycleStartTime;
    private int totalPausedSeconds;
    private LocalDateTime pauseStartTime; // จับเวลาตอนหยุดชั่วคราว

    // เพิ่มตัวแปรนี้สำหรับเก็บค่าเฉลี่ยเวลาซัก (คำนวณสดส่งไปหน้าเว็บ ไม่เซฟลง Database)
    @Transient
    private Integer averageCycleMins;

    // Constructor เปล่า
    public Machine() {}

    // Constructor สร้างเครื่องใหม่
    public Machine(String machineNumber) {
        this.machineNumber  = machineNumber;
        this.status         = "IDLE";
        this.doorOpen       = false;
        this.amplitude      = 0.0;
        this.lastUpdated    = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMachineNumber() { return machineNumber; }
    public void setMachineNumber(String machineNumber) {
        this.machineNumber = machineNumber;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDoorOpen() { return doorOpen; }
    public void setDoorOpen(boolean doorOpen) {
        this.doorOpen = doorOpen;
    }

    public double getAmplitude() { return amplitude; }
    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCycleStartTime() { return cycleStartTime; }
    public void setCycleStartTime(LocalDateTime cycleStartTime) {
        this.cycleStartTime = cycleStartTime;
    }

    public LocalDateTime getPauseStartTime() { return pauseStartTime; }
    public void setPauseStartTime(LocalDateTime pauseStartTime) {
        this.pauseStartTime = pauseStartTime;
    }
    public int getTotalPausedSeconds() { return totalPausedSeconds; }
    public void setTotalPausedSeconds(int totalPausedSeconds) {
        this.totalPausedSeconds = totalPausedSeconds;
    }

    public Integer getAverageCycleMins() { return averageCycleMins; }
    public void setAverageCycleMins(Integer averageCycleMins) {
        this.averageCycleMins = averageCycleMins;
    }
    // เพิ่มตัวแปรสำหรับเก็บรหัส LINE UID ของลูกค้าที่รอกดคิว
    private String waitingUserId;

    public String getWaitingUserId() {
        return waitingUserId;
    }

    public void setWaitingUserId(String waitingUserId) {
        this.waitingUserId = waitingUserId;
    }

}
