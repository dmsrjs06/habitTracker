package com.example.demo.habit.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.habit.HabitLog;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    boolean existsByHabitIdAndDate(Long habitId, LocalDate date);
    void deleteByHabitIdAndDate(Long habitId, LocalDate date);
    void deleteByHabitId(Long habitId);
    List<HabitLog> findByHabitIdOrderByDateDesc(Long habitId);
}