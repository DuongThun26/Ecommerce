package com.example.ecommerce.dashboard.service.impl;

import com.example.ecommerce.common.exception.DashboardBusyException;
import com.example.ecommerce.dashboard.config.DashboardDemoProperties;
import com.example.ecommerce.dashboard.config.DashboardProperties;
import com.example.ecommerce.dashboard.dto.DashboardResponse;
import com.example.ecommerce.dashboard.dto.LowStockProductDto;
import com.example.ecommerce.dashboard.dto.TopProductDto;
import com.example.ecommerce.dashboard.dto.TopProductProjection;
import com.example.ecommerce.dashboard.service.DashboardService;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    public static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<OrderStatus> PAID_STATUSES = List.of(
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.COMPLETED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ThreadPoolTaskExecutor dashboardExecutor;
    private final DashboardProperties properties;
    private final DashboardDemoProperties demoProperties;

    @Override
    public DashboardResponse getDashboard(LocalDate date, String mode) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZONE_VN);
        long startTotal = System.currentTimeMillis();

        log.info("Starting dashboard generation for date={}, mode={}", targetDate, mode);

        if ("sequential".equalsIgnoreCase(mode)) {
            return executeSequential(targetDate, startTotal);
        } else {
            return executeParallel(targetDate, startTotal);
        }
    }

    private DashboardResponse executeSequential(LocalDate date, long startTotal) {
        List<String> failedParts = new ArrayList<>();
        boolean partial = false;

        BigDecimal revenueToday = null;
        try {
            revenueToday = executeRevenueToday(date);
        } catch (Exception ex) {
            log.error("Task [revenueToday] failed in sequential mode: {}", ex.getMessage(), ex);
            failedParts.add("revenueToday");
            partial = true;
        }

        Map<String, Long> orderCountByStatus = null;
        try {
            orderCountByStatus = executeOrderCountByStatus();
        } catch (Exception ex) {
            log.error("Task [orderCountByStatus] failed in sequential mode: {}", ex.getMessage(), ex);
            failedParts.add("orderCountByStatus");
            partial = true;
        }

        List<TopProductDto> top5Products = null;
        try {
            top5Products = executeTop5Products(date);
        } catch (Exception ex) {
            log.error("Task [top5Products] failed in sequential mode: {}", ex.getMessage(), ex);
            failedParts.add("top5Products");
            partial = true;
        }

        List<LowStockProductDto> lowStockProducts = null;
        try {
            lowStockProducts = executeLowStockProducts();
        } catch (Exception ex) {
            log.error("Task [lowStockProducts] failed in sequential mode: {}", ex.getMessage(), ex);
            failedParts.add("lowStockProducts");
            partial = true;
        }

        long elapsedMs = System.currentTimeMillis() - startTotal;
        log.info("Sequential dashboard finished in {} ms (partial={}, failedParts={})", elapsedMs, partial, failedParts);

        return DashboardResponse.builder()
                .date(date)
                .revenueToday(revenueToday)
                .orderCountByStatus(orderCountByStatus)
                .top5Products(top5Products)
                .lowStockProducts(lowStockProducts)
                .partial(partial)
                .failedParts(failedParts)
                .elapsedMs(elapsedMs)
                .build();
    }

    private DashboardResponse executeParallel(LocalDate date, long startTotal) {
        long timeoutMs = properties.getDashboard().getPartTimeout().toMillis();
        List<String> failedParts = new ArrayList<>();
        boolean partial = false;

        CompletableFuture<BigDecimal> revenueFuture;
        CompletableFuture<Map<String, Long>> orderCountFuture;
        CompletableFuture<List<TopProductDto>> topProductsFuture;
        CompletableFuture<List<LowStockProductDto>> lowStockFuture;

        try {
            revenueFuture = CompletableFuture.supplyAsync(() -> executeRevenueToday(date), dashboardExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);

            orderCountFuture = CompletableFuture.supplyAsync(this::executeOrderCountByStatus, dashboardExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);

            topProductsFuture = CompletableFuture.supplyAsync(() -> executeTop5Products(date), dashboardExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);

            lowStockFuture = CompletableFuture.supplyAsync(this::executeLowStockProducts, dashboardExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            log.warn("Dashboard executor queue is full, rejecting request: {}", ex.getMessage());
            throw new DashboardBusyException("DASHBOARD_BUSY: System is overloaded", ex);
        }

        // Wait for all 4 tasks to finish or timeout
        try {
            CompletableFuture.allOf(revenueFuture, orderCountFuture, topProductsFuture, lowStockFuture).join();
        } catch (Exception ignored) {
            // Exceptions/Timeouts are caught individually below
        }

        BigDecimal revenueToday = null;
        try {
            revenueToday = revenueFuture.join();
        } catch (Exception ex) {
            log.error("Task [revenueToday] failed or timed out: {}", ex.getMessage(), ex);
            failedParts.add("revenueToday");
            partial = true;
        }

        Map<String, Long> orderCountByStatus = null;
        try {
            orderCountByStatus = orderCountFuture.join();
        } catch (Exception ex) {
            log.error("Task [orderCountByStatus] failed or timed out: {}", ex.getMessage(), ex);
            failedParts.add("orderCountByStatus");
            partial = true;
        }

        List<TopProductDto> top5Products = null;
        try {
            top5Products = topProductsFuture.join();
        } catch (Exception ex) {
            log.error("Task [top5Products] failed or timed out: {}", ex.getMessage(), ex);
            failedParts.add("top5Products");
            partial = true;
        }

        List<LowStockProductDto> lowStockProducts = null;
        try {
            lowStockProducts = lowStockFuture.join();
        } catch (Exception ex) {
            log.error("Task [lowStockProducts] failed or timed out: {}", ex.getMessage(), ex);
            failedParts.add("lowStockProducts");
            partial = true;
        }

        long elapsedMs = System.currentTimeMillis() - startTotal;
        log.info("Parallel dashboard completed in {} ms (partial={}, failedParts={})", elapsedMs, partial, failedParts);

        return DashboardResponse.builder()
                .date(date)
                .revenueToday(revenueToday)
                .orderCountByStatus(orderCountByStatus)
                .top5Products(top5Products)
                .lowStockProducts(lowStockProducts)
                .partial(partial)
                .failedParts(failedParts)
                .elapsedMs(elapsedMs)
                .build();
    }

    private void applyDemoDelayAndFail(String partName) {
        if (demoProperties.getDelay() != null && demoProperties.getDelay().containsKey(partName)) {
            Duration delay = demoProperties.getDelay().get(partName);
            if (delay != null && !delay.isZero() && !delay.isNegative()) {
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Task interrupted: " + partName, e);
                }
            }
        }

        if (demoProperties.getFail() != null && demoProperties.getFail().contains(partName)) {
            log.error("Task [{}] simulated failure triggered by demo profile", partName);
            throw new RuntimeException("Simulated demo failure for part: " + partName);
        }
    }

    private BigDecimal executeRevenueToday(LocalDate date) {
        long start = System.currentTimeMillis();
        applyDemoDelayAndFail("revenueToday");

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        BigDecimal revenue = orderRepository.calculateRevenueBetween(startOfDay, endOfDay, PAID_STATUSES);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Task [revenueToday] completed in {} ms", elapsed);
        return revenue;
    }

    private Map<String, Long> executeOrderCountByStatus() {
        long start = System.currentTimeMillis();
        applyDemoDelayAndFail("orderCountByStatus");

        Map<String, Long> result = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status.name(), 0L);
        }

        List<Object[]> rows = orderRepository.countOrdersGroupByStatus();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] instanceof OrderStatus status && row[1] instanceof Number count) {
                    result.put(status.name(), count.longValue());
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Task [orderCountByStatus] completed in {} ms", elapsed);
        return result;
    }

    private List<TopProductDto> executeTop5Products(LocalDate date) {
        long start = System.currentTimeMillis();
        applyDemoDelayAndFail("top5Products");

        LocalDateTime start30Days = date.minusDays(30).atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<TopProductProjection> projections = orderItemRepository.findTopProductsBetween(
                start30Days,
                endOfDay,
                PAID_STATUSES,
                PageRequest.of(0, 5)
        );

        List<TopProductDto> dtos = new ArrayList<>();
        if (projections != null) {
            for (TopProductProjection p : projections) {
                dtos.add(TopProductDto.builder()
                        .productId(p.getProductId())
                        .name(p.getName())
                        .quantitySold(p.getQuantitySold())
                        .build());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Task [top5Products] completed in {} ms", elapsed);
        return dtos;
    }

    private List<LowStockProductDto> executeLowStockProducts() {
        long start = System.currentTimeMillis();
        applyDemoDelayAndFail("lowStockProducts");

        List<ProductEntity> products = productRepository.findLowStockActiveProducts(5);
        List<LowStockProductDto> dtos = new ArrayList<>();
        if (products != null) {
            for (ProductEntity p : products) {
                dtos.add(LowStockProductDto.builder()
                        .productId(p.getId())
                        .name(p.getName())
                        .stock(p.getStock())
                        .build());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Task [lowStockProducts] completed in {} ms", elapsed);
        return dtos;
    }
}
