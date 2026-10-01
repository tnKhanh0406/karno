package com.karnofleet.controller;

import com.karnofleet.dto.response.FirstMileContext;
import com.karnofleet.service.FirstMileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/first-mile")
@RequiredArgsConstructor
public class FirstMileContextController {

    private final FirstMileContextService contextService;

    @GetMapping("/context")
    public FirstMileContext getContext(@RequestParam UUID userId) {
        return contextService.getContext(userId, LocalDateTime.now());
    }
}