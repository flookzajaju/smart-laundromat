package com.example.dash.repository;

import com.example.dash.model.WashCycle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // 🟢 บรรทัดนี้ที่หายไปครับ

public interface WashCycleRepository extends MongoRepository<WashCycle, String> {

    List<WashCycle> findByMachineNumberOrderByStartTimeDesc(String machineNumber);

    List<WashCycle> findTop10ByOrderByStartTimeDesc();

    List<WashCycle> findByStartTimeAfter(LocalDateTime after);

    Optional<WashCycle> findTopByMachineNumberOrderByStartTimeDesc(String machineNumber);

    List<WashCycle> findByMachineNumberAndStartTimeBetweenOrderByStartTimeAsc(String machineNumber, LocalDateTime start, LocalDateTime end);
}