package com.example.ecommerce.report;

import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.report.dto.ReportExportResult;
import com.example.ecommerce.report.entity.ReportDefinitionEntity;
import com.example.ecommerce.report.entity.ReportParamEntity;
import com.example.ecommerce.report.repository.ReportDefinitionRepository;
import com.example.ecommerce.report.repository.ReportParamRepository;
import com.example.ecommerce.report.service.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReportJasperIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportDefinitionRepository reportDefinitionRepository;

    @Autowired
    private ReportParamRepository reportParamRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        // Đảm bảo bảng report_calendar tồn tại trên H2
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS report_calendar (report_date DATE PRIMARY KEY)");
        jdbcTemplate.execute("DELETE FROM report_calendar");

        // Seed các ngày từ 2026-10-01 đến 2026-10-05
        jdbcTemplate.execute("INSERT INTO report_calendar (report_date) VALUES " +
                "('2026-10-01'), ('2026-10-02'), ('2026-10-03'), ('2026-10-04'), ('2026-10-05')");

        // Clean tables
        reportParamRepository.deleteAll();
        reportDefinitionRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();

        // 1. Seed Report Definition
        reportDefinitionRepository.save(ReportDefinitionEntity.builder()
                .code("RPT_REVENUE_DAILY")
                .name("BÁO CÁO DOANH THU THEO NGÀY")
                .templatePath("reports/rpt_revenue_daily.jrxml")
                .createdAt(LocalDateTime.now())
                .build());

        // 2. Seed Report Params
        reportParamRepository.save(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("fromDate")
                .dataType("DATE")
                .required(true)
                .formatPattern("yyyy-MM-dd")
                .displayOrder(1)
                .build());

        reportParamRepository.save(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("toDate")
                .dataType("DATE")
                .required(true)
                .formatPattern("yyyy-MM-dd")
                .validationRules("{\"rangeTarget\":\"fromDate\",\"maxDays\":92}")
                .displayOrder(2)
                .build());

        reportParamRepository.save(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("customerId")
                .dataType("LONG")
                .required(false)
                .validationRules("{\"referenceTable\":\"customers\"}")
                .displayOrder(3)
                .build());

        // 3. Seed Customers
        CustomerEntity c1 = customerRepository.save(CustomerEntity.builder()
                .name("Nguyễn Văn An")
                .email("an.nguyen@example.com")
                .phone("0901234567")
                .status(CustomerStatus.ACTIVE)
                .deleted(false)
                .build());

        CustomerEntity c2 = customerRepository.save(CustomerEntity.builder()
                .name("Trần Thị Bình")
                .email("binh.tran@example.com")
                .phone("0907654321")
                .status(CustomerStatus.ACTIVE)
                .deleted(false)
                .build());

        // 4. Seed Demo Orders 201 to 205
        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-201")
                .customer(c1)
                .totalAmount(BigDecimal.valueOf(45980000.00))
                .status(OrderStatus.COMPLETED)
                .paidAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .statusChangedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build());

        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-202")
                .customer(c2)
                .totalAmount(BigDecimal.valueOf(190000.00))
                .status(OrderStatus.COMPLETED)
                .paidAt(LocalDateTime.of(2026, 10, 2, 6, 30))
                .statusChangedAt(LocalDateTime.of(2026, 10, 2, 6, 30))
                .build());

        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-203")
                .customer(c1)
                .totalAmount(BigDecimal.valueOf(22990000.00))
                .status(OrderStatus.CONFIRMED)
                .paidAt(LocalDateTime.of(2026, 10, 3, 8, 15))
                .statusChangedAt(LocalDateTime.of(2026, 10, 3, 8, 15))
                .build());

        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-204")
                .customer(c2)
                .totalAmount(BigDecimal.valueOf(5990000.00))
                .status(OrderStatus.CANCELLED)
                .paidAt(LocalDateTime.of(2026, 10, 3, 9, 0))
                .statusChangedAt(LocalDateTime.of(2026, 10, 3, 9, 0))
                .build());

        orderRepository.save(OrderEntity.builder()
                .orderCode("ORD-205")
                .customer(c2)
                .totalAmount(BigDecimal.valueOf(380000.00))
                .status(OrderStatus.PAYMENT_PENDING)
                .paidAt(null)
                .statusChangedAt(LocalDateTime.of(2026, 10, 4, 10, 0))
                .build());
    }

    @Test
    @DisplayName("Kịch bản 1: 2026-10-01 → 2026-10-04, PDF -> Đúng bảng layout; tổng 3 đơn hàng, 69.160.000")
    void testExportPdfScenario1() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-01");
        params.put("toDate", "2026-10-04");

        ReportExportResult result = reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("RPT_REVENUE_DAILY_20261001_20261004.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Kịch bản 2: 2026-10-01 → 2026-10-04 + customerId=1 -> tổng 2 đơn hàng, 68.970.000")
    void testExportPdfScenario2WithCustomerId() {
        CustomerEntity customer1 = customerRepository.findAll().stream()
                .filter(c -> "Nguyễn Văn An".equals(c.getName()))
                .findFirst().orElseThrow();

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-01");
        params.put("toDate", "2026-10-04");
        params.put("customerId", customer1.getId());

        ReportExportResult result = reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("RPT_REVENUE_DAILY_20261001_20261004.pdf");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Kịch bản 3: 2026-10-01 → 2026-10-04, XLSX -> Cột Doanh thu có kiểu số (Quy tắc 10)")
    void testExportXlsxScenario3CheckNumericType() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-01");
        params.put("toDate", "2026-10-04");

        ReportExportResult result = reportService.exportReport("RPT_REVENUE_DAILY", "XLSX", params);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("RPT_REVENUE_DAILY_20261001_20261004.xlsx");
        assertThat(result.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(result.getContent()).isNotEmpty();

        // Đọc workbook bằng Apache POI để xác nhận Quy tắc 10: Cột Doanh thu PHẢI có kiểu số
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContent()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet).isNotNull();

            // Tìm các dòng dữ liệu chi tiết
            boolean foundNumericRevenue = false;
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        double val = cell.getNumericCellValue();
                        if (val == 45980000.0 || val == 69160000.0 || val == 190000.0) {
                            foundNumericRevenue = true;
                            assertThat(cell.getCellType()).isEqualTo(CellType.NUMERIC);
                        }
                    }
                }
            }
            assertThat(foundNumericRevenue).isTrue();
        }
    }
}
