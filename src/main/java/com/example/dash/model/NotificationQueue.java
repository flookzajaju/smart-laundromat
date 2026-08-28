package com.example.dash.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notification_queue")
public class NotificationQueue {

    @Id
    private String id;

    private String machineId;       // รอเครื่องไหน
    private String machineNumber;
    private String userId;          // ใครกดรอ
    private String lineUserId;      // LINE ของคนนั้น
    private LocalDateTime createdAt;
    private boolean notified;       // ส่งแล้วหรือยัง

    // Constructor
    public NotificationQueue() {}

    public NotificationQueue(String machineId, String machineNumber,
                             String userId, String lineUserId) {
        this.machineId = machineId;
        this.machineNumber = machineNumber;
        this.userId = userId;
        this.lineUserId = lineUserId;
        this.createdAt = LocalDateTime.now();
        this.notified = false;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getMachineNumber() { return machineNumber; }
    public void setMachineNumber(String machineNumber) { this.machineNumber = machineNumber; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLineUserId() { return lineUserId; }
    public void setLineUserId(String lineUserId) { this.lineUserId = lineUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}