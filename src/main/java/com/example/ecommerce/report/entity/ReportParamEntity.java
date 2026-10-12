package com.example.ecommerce.report.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "report_param",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_param_code_name", columnNames = {"report_code", "name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportParamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, length = 50)
    private String reportCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_code", referencedColumnName = "code", insertable = false, updatable = false)
    private ReportDefinitionEntity reportDefinition;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType; // DATE, LONG, DECIMAL, STRING

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "format_pattern", length = 50)
    private String formatPattern; // e.g. yyyy-MM-dd

    @Column(name = "validation_rules", length = 500)
    private String validationRules; // JSON format for constraints

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
