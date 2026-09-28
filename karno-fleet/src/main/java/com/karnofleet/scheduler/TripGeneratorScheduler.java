package com.karnofleet.scheduler;

import com.karnofleet.service.TripGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripGeneratorScheduler {

    private final TripGeneratorService tripGeneratorService;

    @Value("${fleet.trip-generator.default-horizon-days:7}")
    private int defaultHorizonDays;

    @Scheduled(cron = "${fleet.trip-generator.cron:0 5 0 * * *}")
    public void scheduleTripGeneration() {
        log.info("Running daily scheduled trip generation for next {} days", defaultHorizonDays);
        try {
            tripGeneratorService.generateNextDays(defaultHorizonDays);
        } catch (Exception e) {
            log.error("Error during scheduled trip generation", e);
        }
    }
}
