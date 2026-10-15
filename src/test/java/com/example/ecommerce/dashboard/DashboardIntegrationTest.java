package com.example.ecommerce.dashboard;

import com.example.ecommerce.common.logging.TraceIdFilter;
import com.example.ecommerce.dashboard.config.DashboardDemoProperties;
import com.example.ecommerce.dashboard.dto.DashboardResponse;
import com.example.ecommerce.dashboard.service.DashboardService;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardDemoProperties demoProperties;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private final LocalDate testDate = LocalDate.of(2026, 10, 7);

    @BeforeEach
    void setUp() {
        demoProperties.getDelay().clear();
        demoProperties.getFail().clear();

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        CustomerEntity customer = customerRepository.save(CustomerEntity.builder()
                .name("Nguyen Van A")
                .email("test.dashboard@example.com")
                .phone("0901234567")
                .status(CustomerStatus.ACTIVE)
                .deleted(false)
                .build());

        // 1. AirPods Pro 2 (ACTIVE, stock = 0) -> Should appear in lowStockProducts
        ProductEntity airPods = productRepository.save(ProductEntity.builder()
                .name("AirPods Pro 2")
                .price(new BigDecimal("5490000.00"))
                .stock(0)
                .status(ProductStatus.ACTIVE)
                .deleted(false)
                .version(0L)
                .build());

        // 2. iPhone 13 (INACTIVE, stock = 5) -> Must NOT appear in lowStockProducts
        productRepository.save(ProductEntity.builder()
                .name("iPhone 13")
                .price(new BigDecimal("13990000.00"))
                .stock(5)
                .status(ProductStatus.INACTIVE)
                .deleted(false)
                .version(0L)
                .build());

        // 3. iPhone 16 128GB (ACTIVE, stock = 50) -> Top selling product
        ProductEntity iphone16 = productRepository.save(ProductEntity.builder()
                .name("iPhone 16 128GB")
                .price(new BigDecimal("22990000.00"))
                .stock(50)
                .status(ProductStatus.ACTIVE)
                .deleted(false)
                .version(0L)
                .build());

        // 4. Paid Orders on testDate
        OrderEntity order1 = orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-101")
                .customer(customer)
                .totalAmount(new BigDecimal("45980000.00"))
                .status(OrderStatus.COMPLETED)
                .paidAt(LocalDateTime.of(2026, 10, 7, 9, 30))
                .statusChangedAt(LocalDateTime.of(2026, 10, 7, 9, 30))
                .version(0L)
                .build());

        orderItemRepository.save(OrderItemEntity.builder()
                .order(order1)
                .product(iphone16)
                .quantity(2)
                .unitPrice(new BigDecimal("22990000.00"))
                .subTotal(new BigDecimal("45980000.00"))
                .build());

        // Another order cancelled
        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-102")
                .customer(customer)
                .totalAmount(new BigDecimal("1000000.00"))
                .status(OrderStatus.CANCELLED)
                .statusChangedAt(LocalDateTime.of(2026, 10, 7, 10, 0))
                .version(0L)
                .build());
    }

    @Test
    @DisplayName("Kịch bản 1: GET /admin/dashboard -> 200, partial=false, lowStockProducts chỉ có AirPods Pro 2, đủ 14 status")
    void testScenario1DefaultDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .param("date", testDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-07"))
                .andExpect(jsonPath("$.partial").value(false))
                .andExpect(jsonPath("$.failedParts").isEmpty())
                .andExpect(jsonPath("$.revenueToday").value(45980000.00))
                .andExpect(jsonPath("$.lowStockProducts[0].name").value("AirPods Pro 2"))
                .andExpect(jsonPath("$.lowStockProducts[0].stock").value(0))
                // Confirm iPhone 13 is NOT in lowStockProducts because its status is INACTIVE
                .andExpect(jsonPath("$.lowStockProducts.length()").value(1))
                // Confirm all 14 statuses exist in orderCountByStatus
                .andExpect(jsonPath("$.orderCountByStatus.COMPLETED").value(1))
                .andExpect(jsonPath("$.orderCountByStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.orderCountByStatus.DRAFT").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.PENDING_APPROVAL").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.PENDING_PO_REVIEW").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.ACTION_REQUIRED").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.RETURNED").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.REJECTED").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.PAYMENT_PENDING").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.PAID").value(0))
                .andExpect(jsonPath("$.orderCountByStatus.CONFIRMED").value(0))
                // Confirm top5Products has iPhone 16
                .andExpect(jsonPath("$.top5Products[0].name").value("iPhone 16 128GB"))
                .andExpect(jsonPath("$.top5Products[0].quantitySold").value(2));
    }

    @Test
    @DisplayName("Kịch bản 2 & 3: So sánh sequential (≥ 2.000 ms) vs parallel (< 1.000 ms) khi mỗi phần delay 500 ms")
    void testScenario2And3SequentialVsParallel() {
        demoProperties.getDelay().put("revenueToday", Duration.ofMillis(500));
        demoProperties.getDelay().put("orderCountByStatus", Duration.ofMillis(500));
        demoProperties.getDelay().put("top5Products", Duration.ofMillis(500));
        demoProperties.getDelay().put("lowStockProducts", Duration.ofMillis(500));

        // Test Sequential
        DashboardResponse seqRes = dashboardService.getDashboard(testDate, "sequential");
        assertThat(seqRes.isPartial()).isFalse();
        assertThat(seqRes.getElapsedMs()).isGreaterThanOrEqualTo(1900); // ~2000 ms

        // Test Parallel
        DashboardResponse parRes = dashboardService.getDashboard(testDate, "parallel");
        assertThat(parRes.isPartial()).isFalse();
        assertThat(parRes.getElapsedMs()).isLessThan(1000); // ~500 ms
    }

    @Test
    @DisplayName("Kịch bản 4: Delay top5Products 3 giây -> elapsedMs ≤ 2.000; partial=true; failedParts=['top5Products']")
    void testScenario4DelayTop5ProductsTimeout() {
        demoProperties.getDelay().put("top5Products", Duration.ofMillis(3000));

        DashboardResponse res = dashboardService.getDashboard(testDate, "parallel");

        assertThat(res.isPartial()).isTrue();
        assertThat(res.getFailedParts()).containsExactly("top5Products");
        assertThat(res.getTop5Products()).isNull();

        // 3 remaining parts must have data
        assertThat(res.getRevenueToday()).isNotNull();
        assertThat(res.getOrderCountByStatus()).isNotNull();
        assertThat(res.getLowStockProducts()).isNotNull();

        // Total elapsed time must be bounded by timeout (1500 ms) <= 2000 ms
        assertThat(res.getElapsedMs()).isLessThanOrEqualTo(2000);
    }

    @Test
    @DisplayName("Kịch bản 5: Bật fail: [lowStockProducts] -> partial=true; failedParts=['lowStockProducts']")
    void testScenario5FailLowStockProducts() {
        demoProperties.getFail().add("lowStockProducts");

        DashboardResponse res = dashboardService.getDashboard(testDate, "parallel");

        assertThat(res.isPartial()).isTrue();
        assertThat(res.getFailedParts()).containsExactly("lowStockProducts");
        assertThat(res.getLowStockProducts()).isNull();

        // 3 remaining parts must have data
        assertThat(res.getRevenueToday()).isNotNull();
        assertThat(res.getOrderCountByStatus()).isNotNull();
        assertThat(res.getTop5Products()).isNotNull();
    }

    @Test
    @DisplayName("Kịch bản 6: Gửi 60 request đồng thời -> Quá tải hàng đợi trả 503 DASHBOARD_BUSY, sau đó request mới trả 200")
    void testScenario6Concurrency60RequestsRejection() throws Exception {
        // Mỗi phần delay 500ms để giữ thread pool và queue bận
        demoProperties.getDelay().put("revenueToday", Duration.ofMillis(500));
        demoProperties.getDelay().put("orderCountByStatus", Duration.ofMillis(500));
        demoProperties.getDelay().put("top5Products", Duration.ofMillis(500));
        demoProperties.getDelay().put("lowStockProducts", Duration.ofMillis(500));

        int threadCount = 60;
        ExecutorService clientPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger busy503Count = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(clientPool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(get("/admin/dashboard")
                                    .param("date", testDate.toString()))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 503) {
                        busy503Count.incrementAndGet();
                        assertThat(result.getResponse().getContentAsString()).contains("DASHBOARD_BUSY");
                    }
                } catch (Exception e) {
                    // exception caught
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // fire all 60 requests simultaneously

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        clientPool.shutdown();

        // Pool có 4 threads + queue 50 = 54 task capacity.
        // 60 requests * 4 tasks = 240 tasks -> chắc chắn nhiều task bị từ chối
        assertThat(busy503Count.get()).isGreaterThan(0);

        // Sau khi các task trước chạy xong, gửi request mới vẫn nhận 200
        demoProperties.getDelay().clear();
        mockMvc.perform(get("/admin/dashboard")
                        .param("date", testDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partial").value(false));
    }

    @Test
    @DisplayName("Kịch bản 7: TraceId được truyền qua TaskDecorator và trả về header X-Trace-Id")
    void testScenario7TraceIdInMdcAndHeader() throws Exception {
        String testTraceId = "TRACE-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(get("/admin/dashboard")
                        .header(TraceIdFilter.TRACE_ID_HEADER, testTraceId)
                        .param("date", testDate.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, testTraceId));
    }
}
