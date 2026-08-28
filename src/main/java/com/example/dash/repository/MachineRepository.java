package com.example.dash.repository;

import com.example.dash.model.Machine;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MachineRepository
        extends MongoRepository<Machine, String> {

    Optional<Machine> findByMachineNumber(String machineNumber);
    List<Machine> findAllByOrderByMachineNumberAsc();
}