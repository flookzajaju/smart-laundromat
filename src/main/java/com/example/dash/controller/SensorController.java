package com.example.dash.controller;

import com.example.dash.model.Machine;
import com.example.dash.model.WashCycle;
import com.example.dash.repository.MachineRepository;
import com.example.dash.repository.WashCycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sensor")
public class SensorController {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private WashCycleRepository washCycleRepository;

    @Autowired
    private com.example.dash.service.LineBotService lineBotService;

    // ===== Config =====
    private static final double RUNNING_THRESHOLD     = 0.04;
    private static final int    PAUSE_TIMEOUT_MINUTES = 1;
    private static final int    RUNNING_DELAY_SECONDS = 4;

    // ===== Buffer เก็บค่าชั่วคราวใน RAM =====
    private final Map<String, List<Double>> axBuffer = new ConcurrentHashMap<>();   // เก็บ Amplitude (สำหรับกราฟดั้งเดิม และ คำนวณ RMS)
    private final Map<String, List<Double>> waveBuffer = new ConcurrentHashMap<>(); // 🟢 เพิ่ม: เก็บ ax (สำหรับวาดกราฟคลื่นสมูท)

    // ===== Map สำหรับเก็บเวลาเริ่มสั่นครั้งแรกของแต่ละเครื่อง =====
    private final Map<String, LocalDateTime> vibrationStartMap = new ConcurrentHashMap<>();


    @PostMapping("/data")
    public ResponseEntity<?> receiveSensorData(@RequestBody Map<String, Object> data) {

        String machineNumber = (String) data.get("machineNumber");
        double amplitude     = toDouble(data.get("amplitude"));
        double maxAmplitude  = toDouble(data.get("maxAmplitude"));
        double ax            = toDouble(data.get("ax"));
        double ay            = toDouble(data.get("ay"));
        double az            = toDouble(data.get("az"));
        double rmsInput      = toDouble(data.get("rms"));
        boolean doorOpen     = data.get("doorOpen") != null && (boolean) data.get("doorOpen");

        if (machineNumber == null || machineNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "machineNumber is required"));
        }

        // หาเครื่องใน MongoDB หรือสร้างใหม่
        Machine machine = machineRepository
                .findByMachineNumber(machineNumber)
                .orElse(new Machine(machineNumber));

        String oldStatus = machine.getStatus();
        String newStatus = determineStatus(amplitude, doorOpen, machine);

        // ===== จัดการ Transition =====

        // 1. IDLE/DONE → RUNNING = เริ่มรอบใหม่
        if (("IDLE".equals(oldStatus) || "DONE".equals(oldStatus) || oldStatus == null) && "RUNNING".equals(newStatus)) {
            machine.setCycleStartTime(LocalDateTime.now());
            machine.setTotalPausedSeconds(0);
            machine.setPauseStartTime(null);

            // สร้าง Buffer ใหม่ที่ปลอดภัยต่อการเข้าถึงหลาย Thread (เคลียร์ทั้ง 2 ถัง)
            axBuffer.put(machineNumber, Collections.synchronizedList(new ArrayList<>()));
            waveBuffer.put(machineNumber, Collections.synchronizedList(new ArrayList<>())); // 🟢 เคลียร์ถังคลื่น
            log(machineNumber, "เริ่มซักรอบใหม่");
        }

        // 2. RUNNING → PAUSED หรือ WAITING = เริ่มจับเวลาพัก
        if ("RUNNING".equals(oldStatus) && ("PAUSED".equals(newStatus) || "WAITING".equals(newStatus))) {
            machine.setPauseStartTime(LocalDateTime.now());
            if ("WAITING".equals(newStatus)) {
                log(machineNumber, "⚠️ มีการเปิดฝาก่อนซักผ้าเสร็จ (WAITING)");
            } else {
                log(machineNumber, "หยุดชั่วคราว (ถ่ายน้ำ/แช่ผ้า)");
            }
        }

        // 3. PAUSED/WAITING → RUNNING = สะสมเวลาพัก แล้วซักต่อ
        if (("PAUSED".equals(oldStatus) || "WAITING".equals(oldStatus)) && "RUNNING".equals(newStatus)) {
            if (machine.getPauseStartTime() != null) {
                long pausedSec = java.time.Duration.between(machine.getPauseStartTime(), LocalDateTime.now()).getSeconds();
                machine.setTotalPausedSeconds(machine.getTotalPausedSeconds() + (int) pausedSec);
            }
            machine.setPauseStartTime(null);
            log(machineNumber, "กลับมาซักต่อ");
        }

        // แจ้งเตือน Error
        if (!"ERROR".equals(oldStatus) && "ERROR".equals(newStatus)) {
            log(machineNumber, "❌ ERROR: เครื่องทำงานค้างเกิน 60 นาที หรือเซ็นเซอร์ขัดข้อง!");
            sendLineAlert("U2114e75c04caea1f16f5e3c803b67a17", "🚨 [ด่วน] เครื่อง " + machineNumber + " ทำงานค้างเกิน 60 นาที เซ็นเซอร์อาจหลุด กรุณาตรวจสอบ!");
        }

        // 4. → DONE = ซักเสร็จ บันทึกรอบ
        if (!"DONE".equals(oldStatus) && "DONE".equals(newStatus)) {
            // 1. คำนวณเวลา Pause สะสม
            if (machine.getPauseStartTime() != null) {
                long pausedSec = java.time.Duration.between(machine.getPauseStartTime(), LocalDateTime.now()).getSeconds();
                machine.setTotalPausedSeconds(machine.getTotalPausedSeconds() + (int) pausedSec);
            }

            // 2. ประมวลผลข้อมูลและบันทึกลงฐานข้อมูล (ใช้ถัง axBuffer ที่เป็น Amplitude)
            List<Double> buf = axBuffer.getOrDefault(machineNumber, new ArrayList<>());
            double rms = calculateRMS(buf);
            saveWashCycle(machine, maxAmplitude, amplitude, rms, buf);

            // 3. Reset ค่าตัวแปรเครื่อง (เคลียร์ RAM)
            machine.setPauseStartTime(null);
            machine.setTotalPausedSeconds(0);
            axBuffer.remove(machineNumber);
            waveBuffer.remove(machineNumber); // 🟢 เคลียร์ถังคลื่นออกด้วย

            log(machineNumber, "ซักเสร็จแล้ว บันทึกรอบ | RMS = " + String.format("%.4f", rms));

            // 4. ระบบแจ้งเตือน LINE แบบป้องกันการส่งซ้ำ
            Set<String> notifiedUids = new HashSet<>();

            sendLineAlert("U2114e75c04caea1f16f5e3c803b67a17", "⚙️ [Admin] เครื่อง " + machineNumber + " ซักเสร็จแล้ว!");
            notifiedUids.add("U2114e75c04caea1f16f5e3c803b67a17");

            if (machine.getWaitingUserId() != null && !machine.getWaitingUserId().isEmpty()) {
                String targetUid = machine.getWaitingUserId();
                if (notifiedUids.add(targetUid)) {
                    sendLineAlert(targetUid, "🔔 แจ้งเตือน: เครื่อง " + machineNumber + " ซักเสร็จแล้วครับ! มารับผ้าได้เลย 👕");
                }
            }
        }

        // 5. DONE → IDLE = เครื่องว่างอีกครั้ง (คนเอาผ้าออกไปแล้ว)
        if ("DONE".equals(oldStatus) && "IDLE".equals(newStatus)) {
            machine.setCycleStartTime(null);
            machine.setTotalPausedSeconds(0);
            log(machineNumber, "เครื่องว่างแล้ว");

            if (machine.getWaitingUserId() != null && !machine.getWaitingUserId().isEmpty()) {
                sendLineAlert(machine.getWaitingUserId(), "🔔 แจ้งเตือนคิว: ตู้ซักผ้าเบอร์ " + machineNumber + " ว่างพร้อมใช้งานแล้วครับ! รีบมาเลยยย 🧺");
                machine.setWaitingUserId(null);
            }
        }

        // ===== เก็บข้อมูลลง buffer ทั้ง 2 ถัง =====
        if ("RUNNING".equals(newStatus) || "PAUSED".equals(newStatus)) {
            // ถัง 1: เก็บ Amplitude สำหรับหน้าปัดหลัก
            List<Double> buf = axBuffer.computeIfAbsent(machineNumber, k -> Collections.synchronizedList(new ArrayList<>()));
            synchronized (buf) {
                buf.add(amplitude);
                if (buf.size() > 7200) {
                    buf.remove(0);
                }
            }

            // ถัง 2: 🟢 เก็บ ax ล้วนๆ สำหรับกราฟคลื่นสมูท
            List<Double> waveBuf = waveBuffer.computeIfAbsent(machineNumber, k -> Collections.synchronizedList(new ArrayList<>()));
            synchronized (waveBuf) {
                waveBuf.add(ax);
                if (waveBuf.size() > 7200) {
                    waveBuf.remove(0);
                }
            }
        }

        // ===== อัปเดตข้อมูลเครื่องลง MongoDB =====
        machine.setAmplitude(amplitude);
        machine.setDoorOpen(doorOpen);
        machine.setStatus(newStatus);
        machine.setLastUpdated(LocalDateTime.now());
        machineRepository.save(machine);

        // ===== คำนวณเวลาซักสำหรับส่งกลับ ESP32 =====
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("machineStatus", newStatus);

        if (machine.getCycleStartTime() != null && ("RUNNING".equals(newStatus) || "PAUSED".equals(newStatus))) {
            long totalSec = java.time.Duration.between(machine.getCycleStartTime(), LocalDateTime.now()).getSeconds();
            long activeSec = totalSec - machine.getTotalPausedSeconds();

            if ("PAUSED".equals(newStatus) && machine.getPauseStartTime() != null) {
                activeSec -= java.time.Duration.between(machine.getPauseStartTime(), LocalDateTime.now()).getSeconds();
            }
            response.put("elapsedSeconds", Math.max(activeSec, 0));
        }

        return ResponseEntity.ok(response);
    }

    // ===== 🟢 ดึง buffer สำหรับกราฟ realtime (แพ็กส่งไปทั้ง 2 แบบ) =====
    @GetMapping("/buffer/{machineNumber}")
    public ResponseEntity<?> getBuffer(@PathVariable String machineNumber) {
        List<Double> buf = axBuffer.getOrDefault(machineNumber, Collections.emptyList());
        List<Double> waveBuf = waveBuffer.getOrDefault(machineNumber, Collections.emptyList());

        List<Double> recentAmp;
        List<Double> recentWave;

        synchronized (buf) {
            recentAmp = buf.size() > 60 ? new ArrayList<>(buf.subList(buf.size() - 60, buf.size())) : new ArrayList<>(buf);
        }

        synchronized (waveBuf) {
            recentWave = waveBuf.size() > 60 ? new ArrayList<>(waveBuf.subList(waveBuf.size() - 60, waveBuf.size())) : new ArrayList<>(waveBuf);
        }

        return ResponseEntity.ok(Map.of(
                "machineNumber", machineNumber,
                "data", recentAmp,   // คืนค่า Amplitude สำหรับกราฟคลาสสิก
                "wave", recentWave,  // คืนค่า ax สำหรับกราฟสมูท
                "count", recentAmp.size()
        ));
    }

    // ===== ดึงข้อมูลประวัติตามวันที่เลือก =====
    @GetMapping("/history/{machineNumber}/date/{dateStr}")
    public ResponseEntity<?> getHistoryByDate(
            @PathVariable String machineNumber,
            @PathVariable String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            List<WashCycle> cycles = washCycleRepository
                    .findByMachineNumberAndStartTimeBetweenOrderByStartTimeAsc(machineNumber, startOfDay, endOfDay);
            return ResponseEntity.ok(cycles);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "รูปแบบวันที่ไม่ถูกต้อง"));
        }
    }

    // ===== ดึงข้อมูลประวัติรอบล่าสุด =====
    @GetMapping("/history/latest/{machineNumber}")
    public ResponseEntity<?> getLatestHistory(@PathVariable String machineNumber) {
        Optional<WashCycle> cycle = washCycleRepository.findTopByMachineNumberOrderByStartTimeDesc(machineNumber);
        return cycle.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String determineStatus(double amplitude, boolean doorOpen, Machine machine) {
        String current = machine.getStatus() != null ? machine.getStatus() : "IDLE";
        String machineNumber = machine.getMachineNumber();



        if (amplitude >= RUNNING_THRESHOLD) {
            if (doorOpen) {
                vibrationStartMap.remove(machineNumber);
                if ("RUNNING".equals(current) || "PAUSED".equals(current) || "WAITING".equals(current)) {
                    return "WAITING";
                }
                return "IDLE";
            }

            if ("RUNNING".equals(current)) return "RUNNING";

            LocalDateTime firstVibeTime = vibrationStartMap.computeIfAbsent(machineNumber, k -> LocalDateTime.now());
            long secondsPassed = java.time.Duration.between(firstVibeTime, LocalDateTime.now()).getSeconds();

            if (secondsPassed >= RUNNING_DELAY_SECONDS) {
                vibrationStartMap.remove(machineNumber);
                return "RUNNING";
            }
            return current;
        }

        vibrationStartMap.remove(machineNumber);

        if ("IDLE".equals(current)) return "IDLE";

        if ("RUNNING".equals(current) && doorOpen) {
            return "WAITING";
        }

        if ("RUNNING".equals(current)) return "PAUSED";

        if ("PAUSED".equals(current) || "WAITING".equals(current)) {
            if ("PAUSED".equals(current) && doorOpen) {
                return "DONE";
            }
            if ("WAITING".equals(current) && !doorOpen) {
                return "PAUSED";
            }
            if (machine.getPauseStartTime() != null) {
                long pausedMinutes = java.time.Duration.between(machine.getPauseStartTime(), LocalDateTime.now()).toMinutes();
                if (pausedMinutes >= PAUSE_TIMEOUT_MINUTES) {
                    return "DONE";
                }
            }
            return current;
        }

        if ("DONE".equals(current)) {
            if (doorOpen) return "IDLE";
            return "DONE";
        }

        return current;
    }

    // ===== บันทึก WashCycle =====
    private void saveWashCycle(Machine machine, double maxAmp, double avgAmp, double rms, List<Double> vBuf) {
        if (machine.getCycleStartTime() == null) return;

        WashCycle cycle = new WashCycle(machine.getId(), machine.getMachineNumber());
        cycle.setStartTime(machine.getCycleStartTime());
        cycle.finishCycle();
        cycle.setAvgAmplitude(avgAmp);
        cycle.setMaxAmplitude(maxAmp);
        cycle.setRmsValue(rms);

// 1. เซฟ Amplitude (ข้อมูลเดิม)
        synchronized (vBuf) {
            cycle.setVibrationHistory(new ArrayList<>(vBuf));
        }

        // 2. 🟢 เซฟ axHistory (ข้อมูลใหม่)
        List<Double> wBuf = waveBuffer.getOrDefault(machine.getMachineNumber(), new ArrayList<>());
        synchronized (wBuf) {
            cycle.setAxHistory(new ArrayList<>(wBuf)); // 🟢 ใส่ค่า ax ลงไป
        }
        int activeDuration = cycle.getDurationMinutes() - (machine.getTotalPausedSeconds() / 60);
        cycle.setActiveDurationMinutes(Math.max(activeDuration, 0));

        washCycleRepository.save(cycle);
        log(machine.getMachineNumber(), "บันทึกรอบซัก | รวม " + cycle.getDurationMinutes() + " นาที | ซักจริง " + cycle.getActiveDurationMinutes() + " นาที");
    }

    // ===== Helper Methods =====
    private double calculateRMS(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sumSq;
        synchronized (values) {
            sumSq = values.stream().mapToDouble(v -> v * v).sum();
        }
        return Math.sqrt(sumSq / values.size());
    }

    private void sendLineAlert(String userId, String message) {
        try {
            lineBotService.sendPushMessage(userId, message);
            System.out.println("✅ ส่ง LINE สำเร็จ UID: " + userId);
        } catch (Exception e) {
            System.err.println("❌ ส่ง LINE พลาด UID: " + userId + " | Error: " + e.getMessage());
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double)  return (Double) val;
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val instanceof Long)    return ((Long) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private void log(String machine, String msg) {
        System.out.println("[" + LocalDateTime.now().toLocalTime().toString().substring(0, 8) + "] [" + machine + "] " + msg);
    }
}