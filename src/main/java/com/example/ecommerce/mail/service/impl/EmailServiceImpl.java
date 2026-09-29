package com.example.ecommerce.mail.service.impl;

import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.mail.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0 VND", symbols);
    }

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.invoice.store-name:E-COMMERCE STORE}")
    private String storeName;

    @Value("${app.invoice.store-email:support@ecommerce.local}")
    private String storeEmail;

    @Value("${app.invoice.store-phone:+84 976 570 703}")
    private String storePhone;

    @Value("${app.invoice.store-address:Tân Mỹ, Nam Từ Liêm, TP Hà Nội}")
    private String storeAddress;

    @Override
    public void sendEmail(String to, String subject, String contentHtml) {
        sendEmailWithAttachment(to, subject, contentHtml, null, null, null);
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String contentHtml, String attachmentFileName, byte[] attachmentBytes, String contentType) {
        if (to == null || to.isBlank()) {
            log.warn("Cannot send email: recipient address is empty.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean isMultipart = attachmentBytes != null && attachmentBytes.length > 0 && attachmentFileName != null;
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, isMultipart, "UTF-8");

            String sender = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : storeEmail;
            helper.setFrom(sender, storeName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(contentHtml, true);

            if (isMultipart) {
                ByteArrayResource resource = new ByteArrayResource(attachmentBytes);
                if (contentType != null && !contentType.isBlank()) {
                    helper.addAttachment(attachmentFileName, resource, contentType);
                } else {
                    helper.addAttachment(attachmentFileName, resource);
                }
            }

            mailSender.send(mimeMessage);
            log.info("Email successfully sent to '{}' with subject: '{}'", to, subject);
        } catch (MessagingException ex) {
            log.error("MessagingException while sending email to '{}': {}", to, ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to '{}': {}", to, ex.getMessage(), ex);
        }
    }

    @Override
    public void sendInvoiceEmail(InvoiceEntity invoice, byte[] invoicePdfBytes) {
        if (invoice == null) {
            log.warn("Cannot send invoice email: invoice is null.");
            return;
        }

        String recipientEmail = invoice.getBillingEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send invoice email: invoice #{} has no billing email.", invoice.getInvoiceNumber());
            return;
        }

        String subject = String.format("[%s] Xác nhận đơn hàng & Hóa đơn điện tử #%s", storeName, invoice.getInvoiceNumber());
        String emailContent = buildInvoiceEmailHtml(invoice);
        String attachmentFileName = String.format("HoaDon_%s.pdf", invoice.getInvoiceNumber());

        sendEmailWithAttachment(
                recipientEmail,
                subject,
                emailContent,
                attachmentFileName,
                invoicePdfBytes,
                "application/pdf"
        );
    }

    private String buildInvoiceEmailHtml(InvoiceEntity invoice) {
        StringBuilder itemsHtml = new StringBuilder();
        int index = 1;
        if (invoice.getItems() != null) {
            for (InvoiceItemEntity item : invoice.getItems()) {
                String bg = (index % 2 == 0) ? "#f8fafc" : "#ffffff";
                itemsHtml.append(String.format(
                        """
                        <tr style="background-color: %s;">
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #64748b;">%d</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">%s</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #334155;">%d</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #334155;">%s</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: 600; color: #0f172a;">%s</td>
                        </tr>
                        """,
                        bg,
                        index++,
                        escapeHtml(item.getProductName()),
                        item.getQuantity() != null ? item.getQuantity() : 1,
                        formatCurrency(item.getUnitPrice()),
                        formatCurrency(item.getSubtotal())
                ));
            }
        }

        String orderIdStr = (invoice.getOrder() != null && invoice.getOrder().getId() != null)
                ? String.valueOf(invoice.getOrder().getId())
                : "N/A";

        String issueDateStr = (invoice.getIssueDate() != null)
                ? invoice.getIssueDate().format(DATE_TIME_FORMATTER)
                : "";

        String paymentMethodStr = (invoice.getPaymentMethod() != null)
                ? invoice.getPaymentMethod().name()
                : "Online";

        StringBuilder discountRow = new StringBuilder();
        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountRow.append(String.format(
                    """
                    <tr>
                        <td style="padding: 6px 12px; color: #64748b; text-align: right;">Giảm giá:</td>
                        <td style="padding: 6px 12px; text-align: right; color: #16a34a; font-weight: 500;">-%s</td>
                    </tr>
                    """,
                    formatCurrency(invoice.getDiscountAmount())
            ));
        }

        StringBuilder taxRow = new StringBuilder();
        if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            taxRow.append(String.format(
                    """
                    <tr>
                        <td style="padding: 6px 12px; color: #64748b; text-align: right;">Thuế VAT:</td>
                        <td style="padding: 6px 12px; text-align: right; color: #334155; font-weight: 500;">%s</td>
                    </tr>
                    """,
                    formatCurrency(invoice.getTaxAmount())
            ));
        }

        return String.format(
                """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Hóa đơn điện tử</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #334155; -webkit-font-smoothing: antialiased;">
                    <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 30px 15px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" max-width="640" style="max-width: 640px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%); padding: 32px 30px; text-align: center; color: #ffffff;">
                                            <h1 style="margin: 0 0 8px 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;">%s</h1>
                                            <p style="margin: 0; font-size: 14px; opacity: 0.9;">CẢM ƠN QUÝ KHÁCH ĐÃ MUA HÀNG</p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 30px;">
                                            <p style="margin: 0 0 16px 0; font-size: 16px; color: #1e293b;">
                                                Kính chào <strong>%s</strong>,
                                            </p>
                                            <p style="margin: 0 0 24px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                                                Đơn hàng <strong>#%s</strong> của quý khách đã được xác nhận thanh toán thành công. Dưới đây là thông tin chi tiết hóa đơn thanh toán:
                                            </p>
                                            
                                            <!-- Info Card -->
                                            <table width="100%%" style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 24px;" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">MÃ HÓA ĐƠN</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">NGÀY PHÁT HÀNH</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">PHƯƠNG THỨC THANH TOÁN</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">TRẠNG THÁI</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #16a34a;">ĐÃ THANH TOÁN</div>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- Items Table -->
                                            <table width="100%%" style="border-collapse: collapse; margin-bottom: 20px;" cellspacing="0" cellpadding="0">
                                                <thead>
                                                    <tr style="background-color: #f1f5f9;">
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: center; font-size: 12px; font-weight: 600; color: #475569;">#</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: left; font-size: 12px; font-weight: 600; color: #475569;">Sản phẩm</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: center; font-size: 12px; font-weight: 600; color: #475569;">SL</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: right; font-size: 12px; font-weight: 600; color: #475569;">Đơn giá</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: right; font-size: 12px; font-weight: 600; color: #475569;">Thành tiền</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    %s
                                                </tbody>
                                            </table>
                                            
                                            <!-- Summary -->
                                            <table width="100%%" cellspacing="0" cellpadding="0" style="margin-bottom: 24px;">
                                                <tr>
                                                    <td width="50%%"></td>
                                                    <td width="50%%">
                                                        <table width="100%%" cellspacing="0" cellpadding="0">
                                                            <tr>
                                                                <td style="padding: 6px 12px; color: #64748b; text-align: right; font-size: 13px;">Tạm tính:</td>
                                                                <td style="padding: 6px 12px; text-align: right; color: #334155; font-size: 13px; font-weight: 500;">%s</td>
                                                            </tr>
                                                            %s
                                                            %s
                                                            <tr>
                                                                <td style="padding: 12px 12px; border-top: 2px solid #e2e8f0; color: #0f172a; text-align: right; font-size: 15px; font-weight: 700;">Tổng cộng:</td>
                                                                <td style="padding: 12px 12px; border-top: 2px solid #e2e8f0; text-align: right; color: #2563eb; font-size: 18px; font-weight: 700;">%s</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- Attachment Note -->
                                            <div style="background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 14px 16px; border-radius: 6px; margin-bottom: 24px;">
                                                <p style="margin: 0; font-size: 13px; color: #1e40af; line-height: 1.5;">
                                                    📎 <strong>File đính kèm:</strong> Bản in hóa đơn điện tử chính thức định dạng PDF (<strong>HoaDon_%s.pdf</strong>) đã được đính kèm trong thư này. Quý khách có thể tải về để lưu trữ hoặc in ấn.
                                                </p>
                                            </div>
                                            
                                            <p style="margin: 0; font-size: 13px; color: #64748b; line-height: 1.5;">
                                                Nếu quý khách có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi qua hotline: <strong>%s</strong> hoặc email: <strong>%s</strong>.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 30px; text-align: center; font-size: 12px; color: #94a3b8;">
                                            <p style="margin: 0 0 4px 0; font-weight: 600; color: #64748b;">%s</p>
                                            <p style="margin: 0 0 4px 0;">Địa chỉ: %s</p>
                                            <p style="margin: 0;">Email này được gửi tự động, vui lòng không phản hồi trực tiếp vào địa chỉ này.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                escapeHtml(storeName),
                escapeHtml(invoice.getBillingName() != null ? invoice.getBillingName() : "Quý khách"),
                orderIdStr,
                escapeHtml(invoice.getInvoiceNumber()),
                issueDateStr,
                paymentMethodStr,
                itemsHtml.toString(),
                formatCurrency(invoice.getSubtotal()),
                discountRow.toString(),
                taxRow.toString(),
                formatCurrency(invoice.getTotalAmount()),
                escapeHtml(invoice.getInvoiceNumber()),
                escapeHtml(storePhone),
                escapeHtml(storeEmail),
                escapeHtml(storeName),
                escapeHtml(storeAddress)
        );
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
