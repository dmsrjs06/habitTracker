package com.example.demo.habit.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.habit.Habit;

public interface HabitRepository extends JpaRepository<Habit, Long> {}