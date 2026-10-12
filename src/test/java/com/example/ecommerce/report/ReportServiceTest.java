package com.example.ecommerce.report;

import com.example.ecommerce.common.exception.InvalidReportParamsException;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.report.dto.ReportParamErrorDto;
import com.example.ecommerce.report.entity.ReportDefinitionEntity;
import com.example.ecommerce.report.entity.ReportParamEntity;
import com.example.ecommerce.report.repository.ReportDefinitionRepository;
import com.example.ecommerce.report.repository.ReportParamRepository;
import com.example.ecommerce.report.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportDefinitionRepository reportDefinitionRepository;
    @Mock
    private ReportParamRepository reportParamRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private DataSource dataSource;

    @InjectMocks
    private ReportServiceImpl reportService;

    private List<ReportParamEntity> mockParams;

    @BeforeEach
    void setUp() {
        mockParams = new ArrayList<>();
        mockParams.add(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("fromDate")
                .dataType("DATE")
                .required(true)
                .formatPattern("yyyy-MM-dd")
                .displayOrder(1)
                .build());
        mockParams.add(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("toDate")
                .dataType("DATE")
                .required(true)
                .formatPattern("yyyy-MM-dd")
                .displayOrder(2)
                .build());
        mockParams.add(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("customerId")
                .dataType("LONG")
                .required(false)
                .validationRules("{\"referenceTable\":\"customers\"}")
                .displayOrder(3)
                .build());

        ReportDefinitionEntity def = ReportDefinitionEntity.builder()
                .code("RPT_REVENUE_DAILY")
                .name("BÁO CÁO DOANH THU THEO NGÀY")
                .templatePath("reports/rpt_revenue_daily.jrxml")
                .build();

        org.mockito.Mockito.lenient().when(reportDefinitionRepository.findByCode("RPT_REVENUE_DAILY"))
                .thenReturn(Optional.of(def));
        org.mockito.Mockito.lenient().when(reportParamRepository.findByReportCodeOrderByDisplayOrderAsc("RPT_REVENUE_DAILY"))
                .thenReturn(mockParams);
    }

    @Test
    @DisplayName("Kịch bản 4: Thiếu fromDate, toDate = 07/10/2026 -> 400, 2 lỗi: REQUIRED, INVALID_FORMAT")
    void testValidationMissingFromDateAndInvalidFormatToDate() {
        Map<String, Object> params = new HashMap<>();
        params.put("toDate", "07/10/2026"); // Sai format (phải là yyyy-MM-dd)

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(2);
                    assertThat(errors).extracting(ReportParamErrorDto::getParam)
                            .containsExactlyInAnyOrder("fromDate", "toDate");
                    assertThat(errors).extracting(ReportParamErrorDto::getReason)
                            .containsExactlyInAnyOrder("REQUIRED", "INVALID_FORMAT");
                    ReportParamErrorDto toDateErr = errors.stream().filter(e -> "toDate".equals(e.getParam())).findFirst().orElseThrow();
                    assertThat(toDateErr.getExpected()).isEqualTo("yyyy-MM-dd");
                });
    }

    @Test
    @DisplayName("Kịch bản 5: 2026-10-05 → 2026-10-01 -> 400, toDate: RANGE_INVALID")
    void testValidationRangeInvalid() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-05");
        params.put("toDate", "2026-10-01");

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getParam()).isEqualTo("toDate");
                    assertThat(errors.get(0).getReason()).isEqualTo("RANGE_INVALID");
                });
    }

    @Test
    @DisplayName("Kịch bản 6: 2026-01-01 → 2026-12-31 (vượt quá 92 ngày) -> 400, toDate: RANGE_TOO_LONG")
    void testValidationRangeTooLong() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-01-01");
        params.put("toDate", "2026-12-31");

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getParam()).isEqualTo("toDate");
                    assertThat(errors.get(0).getReason()).isEqualTo("RANGE_TOO_LONG");
                });
    }

    @Test
    @DisplayName("Kịch bản 7: customerId = 999 (không tồn tại) -> 400, customerId: NOT_FOUND")
    void testValidationCustomerNotFound() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-01");
        params.put("toDate", "2026-10-04");
        params.put("customerId", 999);

        when(customerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getParam()).isEqualTo("customerId");
                    assertThat(errors.get(0).getReason()).isEqualTo("NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Kịch bản 8: customerId = 'abc' -> 400, customerId: INVALID_TYPE")
    void testValidationCustomerInvalidType() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-10-01");
        params.put("toDate", "2026-10-04");
        params.put("customerId", "abc");

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", params))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getParam()).isEqualTo("customerId");
                    assertThat(errors.get(0).getReason()).isEqualTo("INVALID_TYPE");
                });
    }

    @Test
    @DisplayName("Kịch bản 9: Thêm param minAmount (DECIMAL) qua metadata DB -> engine tự động validate và nhận diện không cần sửa Java")
    void testDynamicParamAdditionWithoutJavaCodeChange() {
        // Giả lập Reviewer thêm 1 param vào DB trong buổi review
        mockParams.add(ReportParamEntity.builder()
                .reportCode("RPT_REVENUE_DAILY")
                .name("minAmount")
                .dataType("DECIMAL")
                .required(false)
                .displayOrder(4)
                .build());

        // Kiểm tra GET definition trả về param mới
        var definition = reportService.getReportDefinition("RPT_REVENUE_DAILY");
        assertThat(definition.getParams()).hasSize(4);
        assertThat(definition.getParams().stream().anyMatch(p -> "minAmount".equals(p.getName()))).isTrue();

        // Kiểm tra validation với giá trị sai kiểu
        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("fromDate", "2026-10-01");
        invalidParams.put("toDate", "2026-10-04");
        invalidParams.put("minAmount", "not_a_number");

        assertThatThrownBy(() -> reportService.exportReport("RPT_REVENUE_DAILY", "PDF", invalidParams))
                .isInstanceOf(InvalidReportParamsException.class)
                .satisfies(ex -> {
                    List<ReportParamErrorDto> errors = ((InvalidReportParamsException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getParam()).isEqualTo("minAmount");
                    assertThat(errors.get(0).getReason()).isEqualTo("INVALID_TYPE");
                });
    }
}
