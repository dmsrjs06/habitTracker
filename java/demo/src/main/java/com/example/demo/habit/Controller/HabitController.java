package com.example.demo.habit.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.habit.Habit;
import com.example.demo.habit.HabitLog;
import com.example.demo.habit.Repository.HabitLogRepository;
import com.example.demo.habit.Repository.HabitRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitRepository repo;
    private final HabitLogRepository logRepo;

    public record HabitRequest(@NotBlank String name) {}
    public record HabitResponse(Long id, String name, boolean checkedToday, int streak, List<String> recentDates) {}

    @GetMapping
    public List<HabitResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitResponse create(@RequestBody @Valid HabitRequest req) {
        return toResponse(repo.save(new Habit(req.name())));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        logRepo.deleteByHabitId(id);
        repo.deleteById(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public HabitResponse rename(@PathVariable Long id, @RequestBody @Valid HabitRequest req) {
     Habit habit = repo.findById(id)
             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
     habit.rename(req.name().trim());
     return toResponse(habit);
    }

    @PostMapping("/{id}/check")
    @ResponseStatus(HttpStatus.CREATED)
    public void check(@PathVariable Long id,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Habit habit = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate target = date != null ? date : LocalDate.now();
        if (target.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미래 날짜는 체크할 수 없어요");
        }
        if (!logRepo.existsByHabitIdAndDate(id, target)) {
            logRepo.save(new HabitLog(habit, target));
        }
    }

    @DeleteMapping("/{id}/check")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uncheck(@PathVariable Long id,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logRepo.deleteByHabitIdAndDate(id, date != null ? date : LocalDate.now());
    }

    @GetMapping("/{id}/streak")
    public Map<String, Integer> streak(@PathVariable Long id) {
        List<HabitLog> logs = logRepo.findByHabitIdOrderByDateDesc(id);
        return Map.of("streak", calcStreak(logs, LocalDate.now()));
    }

    private HabitResponse toResponse(Habit h) {
        List<HabitLog> logs = logRepo.findByHabitIdOrderByDateDesc(h.getId());
        LocalDate today = LocalDate.now();
        boolean checkedToday = logs.stream().anyMatch(l -> l.getDate().equals(today));
        List<String> recent = logs.stream()
                .map(HabitLog::getDate)
                .filter(d -> !d.isBefore(today.minusDays(6)))
                .map(LocalDate::toString)
                .toList();
        return new HabitResponse(h.getId(), h.getName(), checkedToday, calcStreak(logs, today), recent);
    }

    // 오늘 아직 안 했어도 어제까지 이어졌다면 streak 유지
    private int calcStreak(List<HabitLog> logs, LocalDate today) {
        LocalDate expected = logs.stream().anyMatch(l -> l.getDate().equals(today))
                ? today : today.minusDays(1);
        int count = 0;
        for (HabitLog log : logs) {
            if (log.getDate().equals(expected)) {
                count++;
                expected = expected.minusDays(1);
            } else if (log.getDate().isBefore(expected)) {
                break;
            }
        }
        return count;
    }
}