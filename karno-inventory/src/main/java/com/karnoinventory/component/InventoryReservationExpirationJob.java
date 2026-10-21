package com.karnoinventory.component;

import com.karnoinventory.repository.InventoryReservationRepo;
import com.karnoinventory.service.InventoryReservationExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationExpirationJob {
    private final InventoryReservationRepo reservationRepo;
    private final InventoryReservationExpirationService expirationService;

    @Value("${inventory.reservation-expiration.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${inventory.reservation-expiration.fixed-delay-ms:60000}")
    public void expireReservations() {
        Instant now = Instant.now();
        for (UUID checkoutId : reservationRepo.findExpiredCheckoutIds(now, batchSize)) {
            try {
                expirationService.expireCheckout(checkoutId, now);
            } catch (RuntimeException ex) {
                log.error("Cannot expire inventory reservation checkoutId={}", checkoutId, ex);
            }
        }
    }
}
