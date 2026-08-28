package com.example.dash.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
public class LineBotService {


    private static final String CHANNEL_ACCESS_TOKEN = "py6/ebdFOLlsqA9ej8jg8W/mEAWMhek2RfbQZ6n7uWSuzEdSvt+16qS8jztKuJ+HLhp/03kGcJKnF13mBW5JQcsTiPx3YMqP22yxvoT1iIx7r5hJoTYY9vPD8V0GLTkkkOA4g23PvW1dslXSH7qLfAdB04t89/1O/w1cDnyilFU=";

    public void sendPushMessage(String targetUserId, String messageText) {
        String url = "https://api.line.me/v2/bot/message/push";

        // 1. ตั้งค่า Header เพื่อยืนยันตัวตนกับ LINE
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(CHANNEL_ACCESS_TOKEN);

        // 2. สร้างก้อนข้อความ (Message Object)
        Map<String, Object> textMessage = Map.of(
                "type", "text",
                "text", messageText
        );

        // 3. ระบุว่าจะส่งไปให้ใคร (to) และส่งข้อความอะไรไป (messages)
        Map<String, Object> body = Map.of(
                "to", targetUserId,
                "messages", List.of(textMessage)
        );

        // 4. สั่งยิง Request ไปที่เซิร์ฟเวอร์ของ LINE
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ ส่ง LINE สำเร็จไปยัง: " + targetUserId);
        } catch (Exception e) {
            System.err.println("❌ ส่ง LINE ผิดพลาด: " + e.getMessage());
        }
    }
}