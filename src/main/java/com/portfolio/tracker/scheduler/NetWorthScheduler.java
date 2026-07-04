package com.portfolio.tracker.scheduler;

import com.portfolio.tracker.repository.UserRepository;
import com.portfolio.tracker.service.NetWorthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NetWorthScheduler {

    private final NetWorthService netWorthService;
    private final UserRepository userRepository;

    // Run every day at 11 PM
    @Scheduled(cron = "0 0 23 * * *")
    public void saveDailySnapshots() {
        log.info("Running daily net worth snapshot...");
        userRepository.findAll().forEach(user -> {
            try {
                netWorthService.saveDailySnapshot(user.getId());
            } catch (Exception e) {
                log.error("Failed to save snapshot for user {}: {}", user.getId(), e.getMessage());
            }
        });
        log.info("Daily snapshots completed");
    }
}