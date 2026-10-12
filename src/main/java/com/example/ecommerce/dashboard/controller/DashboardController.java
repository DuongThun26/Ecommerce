package com.example.ecommerce.dashboard.controller;

import com.example.ecommerce.dashboard.dto.DashboardResponse;
import com.example.ecommerce.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Dashboard chạy song song 4 phần dữ liệu với CompletableFuture và MDC TaskDecorator")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Xem dashboard quản trị (hỗ trợ chạy song song hoặc tuần tự)")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(name = "mode", required = false, defaultValue = "parallel")
            String mode) {

        DashboardResponse response = dashboardService.getDashboard(date, mode);
        return ResponseEntity.ok(response);
    }
}
