package com.example.ecommerce.controller;

import com.example.ecommerce.common.config.ResponseWrapperAdvice;
import com.example.ecommerce.common.exception.EmailNotResendableException;
import com.example.ecommerce.common.exception.GlobalExceptionHandler;
import com.example.ecommerce.mail.controller.EmailAdminController;
import com.example.ecommerce.mail.dto.response.EmailOutboxResponse;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.service.EmailOutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ResponseWrapperAdvice.class})
class EmailAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    @Test
    @DisplayName("Quy tắc 9: GET /api/admin/emails?status=FAILED&page=0&size=20 trả danh sách mail lỗi")
    void getFailedEmailsShouldReturnPageResult() throws Exception {
        EmailOutboxResponse item = EmailOutboxResponse.builder()
                .id(1L)
                .template("ORDER_PAID")
                .referenceId(100L)
                .recipient("an.nguyen@example.com")
                .status(EmailOutboxStatus.FAILED)
                .attempts(5)
                .lastError("SMTP Connection Refused")
                .createdAt(LocalDateTime.now())
                .build();

        PageImpl<EmailOutboxResponse> pageResult = new PageImpl<>(List.of(item));
        when(emailOutboxService.getEmailOutboxList(eq(EmailOutboxStatus.FAILED), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/emails")
                        .param("status", "FAILED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].attempts").value(5));
    }

    @Test
    @DisplayName("Quy tắc 10: POST /api/admin/emails/{id}/resend thành công -> trả về 200 OK với status PENDING")
    void resendFailedEmailShouldReturnOk() throws Exception {
        EmailOutboxResponse resendResult = EmailOutboxResponse.builder()
                .id(1L)
                .status(EmailOutboxStatus.PENDING)
                .attempts(0)
                .build();

        when(emailOutboxService.resendFailedEmail(1L)).thenReturn(resendResult);

        mockMvc.perform(post("/api/admin/emails/1/resend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.attempts").value(0));
    }

    @Test
    @DisplayName("Quy tắc 10: POST /api/admin/emails/{id}/resend khi không ở FAILED -> trả về 409 EMAIL_NOT_RESENDABLE")
    void resendNonFailedEmailShouldReturn409() throws Exception {
        when(emailOutboxService.resendFailedEmail(2L))
                .thenThrow(new EmailNotResendableException("Email #2 không ở trạng thái FAILED"));

        mockMvc.perform(post("/api/admin/emails/2/resend"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_RESENDABLE"))
                .andExpect(jsonPath("$.message").value("Email #2 không ở trạng thái FAILED"));
    }
}
