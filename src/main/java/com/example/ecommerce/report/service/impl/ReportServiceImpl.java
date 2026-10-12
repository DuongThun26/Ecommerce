package com.example.ecommerce.report.service.impl;

import com.example.ecommerce.common.exception.InvalidReportParamsException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.report.dto.*;
import com.example.ecommerce.report.entity.ReportDefinitionEntity;
import com.example.ecommerce.report.entity.ReportParamEntity;
import com.example.ecommerce.report.repository.ReportDefinitionRepository;
import com.example.ecommerce.report.repository.ReportParamRepository;
import com.example.ecommerce.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportParamRepository reportParamRepository;
    private final CustomerRepository customerRepository;
    private final DataSource dataSource;

    private static final DateTimeFormatter DATE_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME_EXPORT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // Quy tắc 8: Hệ thống PHẢI compile .jrxml 1 lần và dùng lại bản đã compile.
    private final Map<String, JasperReport> compiledReportCache = new ConcurrentHashMap<>();

    @Override
    public ReportDefinitionResponse getReportDefinition(String reportCode) {
        ReportDefinitionEntity report = reportDefinitionRepository.findByCode(reportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Report with code " + reportCode + " not found."));

        List<ReportParamEntity> params = reportParamRepository.findByReportCodeOrderByDisplayOrderAsc(reportCode);

        List<ReportParamResponse> paramResponses = params.stream()
                .map(p -> ReportParamResponse.builder()
                        .name(p.getName())
                        .dataType(p.getDataType())
                        .required(p.isRequired())
                        .formatPattern(p.getFormatPattern())
                        .validationRules(p.getValidationRules())
                        .build())
                .toList();

        return ReportDefinitionResponse.builder()
                .code(report.getCode())
                .name(report.getName())
                .params(paramResponses)
                .build();
    }

    @Override
    public ReportExportResult exportReport(String reportCode, String format, Map<String, Object> inputParams) {
        ReportDefinitionEntity report = reportDefinitionRepository.findByCode(reportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Report with code " + reportCode + " not found."));

        List<ReportParamEntity> paramDefs = reportParamRepository.findByReportCodeOrderByDisplayOrderAsc(reportCode);

        // Quy tắc 2, 3, 4: Validate và convert các tham số theo ParamSet từ DB
        Map<String, Object> convertedParams = validateAndConvertParams(paramDefs, inputParams);

        // Chuẩn bị tham số cho JasperReports
        Map<String, Object> jasperParams = new HashMap<>(convertedParams);

        LocalDate fromDate = (LocalDate) convertedParams.get("fromDate");
        LocalDate toDate = (LocalDate) convertedParams.get("toDate");

        if (fromDate != null) {
            jasperParams.put("fromDate", Date.valueOf(fromDate));
            jasperParams.put("fromDateStr", fromDate.format(DATE_DISPLAY_FORMATTER));
        }

        if (toDate != null) {
            jasperParams.put("toDate", Date.valueOf(toDate));
            jasperParams.put("toDateStr", toDate.format(DATE_DISPLAY_FORMATTER));
        }

        Long customerId = (Long) convertedParams.get("customerId");
        if (customerId != null) {
            jasperParams.put("customerId", customerId);
            customerRepository.findById(customerId).ifPresent(c -> {
                String name = c.getName() != null ? c.getName() : ("Customer #" + c.getId());
                jasperParams.put("customerName", name);
            });
        } else {
            jasperParams.put("customerId", null);
            jasperParams.put("customerName", "Tất cả");
        }

        ZonedDateTime nowUtc7 = ZonedDateTime.now(VIETNAM_ZONE);
        jasperParams.put("exportTimeStr", nowUtc7.format(DATE_TIME_EXPORT_FORMATTER) + " (UTC+7)");

        // Lấy hoặc compile template report
        JasperReport jasperReport = getOrCompileReport(report.getTemplatePath());

        // Xuất file PDF hoặc XLSX
        byte[] content;
        String normalizedFormat = (format != null) ? format.trim().toUpperCase() : "PDF";

        try (Connection connection = dataSource.getConnection()) {
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, jasperParams, connection);

            if ("XLSX".equals(normalizedFormat)) {
                content = exportToXlsx(jasperPrint);
            } else {
                content = JasperExportManager.exportReportToPdf(jasperPrint);
            }
        } catch (Exception ex) {
            log.error("Failed to generate report {}: {}", reportCode, ex.getMessage(), ex);
            throw new RuntimeException("Lỗi trong quá trình kết xuất báo cáo: " + ex.getMessage(), ex);
        }

        // Quy tắc 11: Tên file: RPT_REVENUE_DAILY_<yyyyMMdd>_<yyyyMMdd>.pdf hoặc .xlsx
        String fromStr = fromDate != null ? fromDate.format(DATE_FILE_FORMATTER) : "from";
        String toStr = toDate != null ? toDate.format(DATE_FILE_FORMATTER) : "to";
        String ext = "XLSX".equals(normalizedFormat) ? "xlsx" : "pdf";
        String fileName = reportCode + "_" + fromStr + "_" + toStr + "." + ext;

        String contentType = "XLSX".equals(normalizedFormat)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        return ReportExportResult.builder()
                .fileName(fileName)
                .contentType(contentType)
                .content(content)
                .build();
    }

    private Map<String, Object> validateAndConvertParams(List<ReportParamEntity> paramDefs, Map<String, Object> inputParams) {
        if (inputParams == null) {
            inputParams = Collections.emptyMap();
        }

        List<ReportParamErrorDto> errors = new ArrayList<>();
        Map<String, Object> converted = new HashMap<>();

        for (ReportParamEntity paramDef : paramDefs) {
            String name = paramDef.getName();
            Object raw = inputParams.get(name);

            if (raw == null || (raw instanceof String s && s.trim().isEmpty())) {
                if (paramDef.isRequired()) {
                    errors.add(new ReportParamErrorDto(name, "REQUIRED"));
                }
                continue;
            }

            String valStr = raw.toString().trim();
            String dataType = paramDef.getDataType().toUpperCase();

            switch (dataType) {
                case "DATE" -> {
                    // Kiểm tra định dạng yyyy-MM-dd
                    if (!DATE_PATTERN.matcher(valStr).matches()) {
                        errors.add(new ReportParamErrorDto(name, "INVALID_FORMAT", paramDef.getFormatPattern() != null ? paramDef.getFormatPattern() : "yyyy-MM-dd"));
                    } else {
                        try {
                            LocalDate parsed = LocalDate.parse(valStr, DATE_INPUT_FORMATTER);
                            converted.put(name, parsed);
                        } catch (Exception ex) {
                            errors.add(new ReportParamErrorDto(name, "INVALID_FORMAT", paramDef.getFormatPattern() != null ? paramDef.getFormatPattern() : "yyyy-MM-dd"));
                        }
                    }
                }
                case "LONG" -> {
                    try {
                        long longVal = Long.parseLong(valStr);
                        converted.put(name, longVal);

                        // Kiểm tra tham chiếu customer phải tồn tại
                        String rules = paramDef.getValidationRules();
                        if (rules != null && rules.contains("customers")) {
                            if (!customerRepository.existsById(longVal)) {
                                errors.add(new ReportParamErrorDto(name, "NOT_FOUND"));
                            }
                        }
                    } catch (NumberFormatException ex) {
                        errors.add(new ReportParamErrorDto(name, "INVALID_TYPE"));
                    }
                }
                case "DECIMAL" -> {
                    try {
                        BigDecimal decimalVal = new BigDecimal(valStr);
                        converted.put(name, decimalVal);
                    } catch (NumberFormatException ex) {
                        errors.add(new ReportParamErrorDto(name, "INVALID_TYPE"));
                    }
                }
                case "STRING" -> converted.put(name, valStr);
                default -> converted.put(name, valStr);
            }
        }

        // Kiểm tra ràng buộc khoảng ngày (toDate >= fromDate và khoảng <= 92 ngày)
        if (converted.containsKey("fromDate") && converted.containsKey("toDate")) {
            LocalDate from = (LocalDate) converted.get("fromDate");
            LocalDate to = (LocalDate) converted.get("toDate");

            if (to.isBefore(from)) {
                errors.add(new ReportParamErrorDto("toDate", "RANGE_INVALID"));
            } else if (ChronoUnit.DAYS.between(from, to) > 92) {
                errors.add(new ReportParamErrorDto("toDate", "RANGE_TOO_LONG"));
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidReportParamsException(errors);
        }

        return converted;
    }

    private JasperReport getOrCompileReport(String templatePath) {
        return compiledReportCache.computeIfAbsent(templatePath, path -> {
            try {
                log.info("Compiling JasperReport template: {}", path);
                ClassPathResource resource = new ClassPathResource(path);
                try (InputStream is = resource.getInputStream()) {
                    return JasperCompileManager.compileReport(is);
                }
            } catch (Exception e) {
                log.error("Failed to compile JasperReport template {}: {}", path, e.getMessage(), e);
                throw new RuntimeException("Không thể biên dịch mẫu báo cáo " + path + ": " + e.getMessage(), e);
            }
        });
    }

    private byte[] exportToXlsx(JasperPrint jasperPrint) throws JRException {
        JRXlsxExporter exporter = new JRXlsxExporter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));

        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        // Quy tắc 10: Trong XLSX, cột Doanh thu PHẢI có kiểu số, không phải text
        configuration.setDetectCellType(true);
        configuration.setRemoveEmptySpaceBetweenRows(true);
        configuration.setWhitePageBackground(false);
        configuration.setAutoFitRow(true);

        exporter.setConfiguration(configuration);
        exporter.exportReport();

        return baos.toByteArray();
    }
}
