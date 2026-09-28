package com.karnohub.controller.internal;

import com.karnohub.entity.Hub;
import com.karnohub.entity.HubConnection;
import com.karnohub.service.HubService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/hubs")
@RequiredArgsConstructor
public class HubInternalController {

    private final HubService hubService;

    @GetMapping
    public ResponseEntity<?> getHubs(@RequestParam(required = false) String code) {
        if (code != null && !code.isBlank()) {
            return hubService.getHubByCode(code.trim())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(hubService.getAllActiveHubs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hub> getHubById(@PathVariable UUID id) {
        return hubService.getHubById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/connections")
    public ResponseEntity<List<HubConnection>> getConnections(@PathVariable UUID id) {
        return ResponseEntity.ok(hubService.getConnectionsFromHub(id));
    }

    @GetMapping("/connections")
    public ResponseEntity<List<HubConnection>> getAllConnections() {
        return ResponseEntity.ok(hubService.getAllActiveConnections());
    }
}
