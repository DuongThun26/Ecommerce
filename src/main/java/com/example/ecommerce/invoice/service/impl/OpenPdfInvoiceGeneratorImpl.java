package com.example.ecommerce.invoice.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.invoice.service.InvoicePdfGenerator;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
public class OpenPdfInvoiceGeneratorImpl implements InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0 VND", symbols);
    }

    @Value("${app.invoice.store-name:E-COMMERCE ENTERPRISE STORE}")
    private String storeName;

    @Value("${app.invoice.store-address:123 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh}")
    private String storeAddress;

    @Value("${app.invoice.store-phone:+84 28 1234 5678}")
    private String storePhone;

    @Value("${app.invoice.store-email:support@ecommerce.local}")
    private String storeEmail;

    /**
     * Load TrueType font with UTF-8 / IDENTITY_H encoding for Vietnamese support
     */
    private BaseFont loadBaseFont(boolean bold, boolean italic) {
        String resourceName = bold ? "arialbd.ttf" : (italic ? "ariali.ttf" : "arial.ttf");
        String resourcePath = "/fonts/" + resourceName;

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                return BaseFont.createFont(resourceName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải font từ classpath {}, thử tải từ đường dẫn hệ thống: {}", resourcePath, e.getMessage());
        }

        // Fallback to Windows system font path
        String systemPath = "C:/Windows/Fonts/" + resourceName;
        try {
            return BaseFont.createFont(systemPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ex) {
            log.warn("Không thể tải font từ đường dẫn hệ thống {}, sử dụng font mặc định HELVETICA: {}", systemPath, ex.getMessage());
        }

        // Final fallback
        try {
            return BaseFont.createFont(
                    bold ? BaseFont.HELVETICA_BOLD : (italic ? BaseFont.HELVETICA_OBLIQUE : BaseFont.HELVETICA),
                    BaseFont.WINANSI,
                    BaseFont.NOT_EMBEDDED
            );
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi khởi tạo BaseFont", ex);
        }
    }

    @Override
    public byte[] generateInvoicePdf(InvoiceEntity invoice) {
        if (invoice == null) {
            throw new BadRequestException("Invoice cannot be null for PDF generation.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            document.open();

            // Unicode TrueType BaseFonts (Hỗ trợ 100% tiếng Việt)
            BaseFont regularBase = loadBaseFont(false, false);
            BaseFont boldBase = loadBaseFont(true, false);
            BaseFont italicBase = loadBaseFont(false, true);

            // Font configurations
            Font brandFont = new Font(boldBase, 14, Font.NORMAL, new Color(79, 70, 229));
            Font titleFont = new Font(boldBase, 18, Font.NORMAL, Color.DARK_GRAY);
            Font sectionTitleFont = new Font(boldBase, 9, Font.NORMAL, new Color(79, 70, 229));
            Font headerFont = new Font(boldBase, 10, Font.NORMAL, Color.WHITE);
            Font boldFont = new Font(boldBase, 9, Font.NORMAL, Color.BLACK);
            Font normalFont = new Font(regularBase, 9, Font.NORMAL, Color.BLACK);
            Font smallFont = new Font(regularBase, 8, Font.NORMAL, Color.GRAY);
            Font totalFont = new Font(boldBase, 11, Font.NORMAL, new Color(79, 70, 229));
            Font thankYouFont = new Font(italicBase, 10, Font.NORMAL, Color.DARK_GRAY);

            // 1. Header: Store info + Invoice Title
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell storeCell = new PdfPCell();
            storeCell.setBorder(Rectangle.NO_BORDER);
            storeCell.addElement(new Paragraph(storeName, brandFont));
            storeCell.addElement(new Paragraph(storeAddress, smallFont));
            storeCell.addElement(new Paragraph("Số ĐT: " + storePhone + " | Email: " + storeEmail, smallFont));
            headerTable.addCell(storeCell);

            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(title);

            Paragraph invNum = new Paragraph("Số HĐ: " + invoice.getInvoiceNumber(), boldFont);
            invNum.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invNum);

            Paragraph issueDate = new Paragraph("Ngày xuất: " + invoice.getIssueDate().format(DATE_TIME_FORMATTER), smallFont);
            issueDate.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(issueDate);

            headerTable.addCell(invoiceMetaCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // 2. Billing & Order Information
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            PdfPCell billToCell = new PdfPCell();
            billToCell.setBorder(Rectangle.BOX);
            billToCell.setBorderColor(new Color(226, 232, 240));
            billToCell.setPadding(8);
            billToCell.setBackgroundColor(new Color(248, 250, 252));
            billToCell.addElement(new Paragraph("THÔNG TIN KHÁCH HÀNG:", sectionTitleFont));
            billToCell.addElement(new Paragraph(invoice.getBillingName() != null ? invoice.getBillingName() : "N/A", boldFont));
            billToCell.addElement(new Paragraph("Email: " + (invoice.getBillingEmail() != null ? invoice.getBillingEmail() : "N/A"), normalFont));
            billToCell.addElement(new Paragraph("Điện thoại: " + (invoice.getBillingPhone() != null ? invoice.getBillingPhone() : "N/A"), normalFont));
            if (invoice.getBillingAddress() != null && !invoice.getBillingAddress().isBlank()) {
                billToCell.addElement(new Paragraph("Địa chỉ: " + invoice.getBillingAddress(), normalFont));
            }
            infoTable.addCell(billToCell);

            PdfPCell orderMetaCell = new PdfPCell();
            orderMetaCell.setBorder(Rectangle.BOX);
            orderMetaCell.setBorderColor(new Color(226, 232, 240));
            orderMetaCell.setPadding(8);
            orderMetaCell.setBackgroundColor(new Color(248, 250, 252));
            orderMetaCell.addElement(new Paragraph("CHI TIẾT ĐƠN HÀNG:", sectionTitleFont));
            orderMetaCell.addElement(new Paragraph("Mã đơn hàng: #" + (invoice.getOrder() != null ? invoice.getOrder().getId() : "N/A"), normalFont));
            orderMetaCell.addElement(new Paragraph("Trạng thái: " + (invoice.getStatus() != null ? invoice.getStatus().name() : "ISSUED"), normalFont));
            orderMetaCell.addElement(new Paragraph("Phương thức: " + (invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "N/A"), normalFont));
            if (invoice.getPaymentTransactionRef() != null) {
                orderMetaCell.addElement(new Paragraph("Mã giao dịch: " + invoice.getPaymentTransactionRef(), smallFont));
            }
            infoTable.addCell(orderMetaCell);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 3. Invoice Items Table
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{8, 44, 10, 18, 20});
            itemsTable.setHeaderRows(1);

            // Table Header
            Color headerBg = new Color(79, 70, 229);
            addHeaderCell(itemsTable, "STT", headerFont, headerBg, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Tên sản phẩm / Hàng hóa", headerFont, headerBg, Element.ALIGN_LEFT);
            addHeaderCell(itemsTable, "SL", headerFont, headerBg, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Đơn giá", headerFont, headerBg, Element.ALIGN_RIGHT);
            addHeaderCell(itemsTable, "Thành tiền", headerFont, headerBg, Element.ALIGN_RIGHT);

            // Table Rows
            int index = 1;
            if (invoice.getItems() != null) {
                for (InvoiceItemEntity item : invoice.getItems()) {
                    Color rowBg = (index % 2 == 0) ? new Color(248, 250, 252) : Color.WHITE;
                    addBodyCell(itemsTable, String.valueOf(index++), normalFont, rowBg, Element.ALIGN_CENTER);
                    addBodyCell(itemsTable, item.getProductName() != null ? item.getProductName() : "Sản phẩm", normalFont, rowBg, Element.ALIGN_LEFT);
                    addBodyCell(itemsTable, String.valueOf(item.getQuantity() != null ? item.getQuantity() : 1), normalFont, rowBg, Element.ALIGN_CENTER);
                    addBodyCell(itemsTable, formatCurrency(item.getUnitPrice()), normalFont, rowBg, Element.ALIGN_RIGHT);
                    addBodyCell(itemsTable, formatCurrency(item.getSubtotal()), normalFont, rowBg, Element.ALIGN_RIGHT);
                }
            }

            document.add(itemsTable);
            document.add(new Paragraph(" "));

            // 4. Totals Summary Table
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(45);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{50, 50});

            addTotalRow(totalsTable, "Tạm tính:", formatCurrency(invoice.getSubtotal()), normalFont, false);
            if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Giảm giá:", "-" + formatCurrency(invoice.getDiscountAmount()), normalFont, false);
            }
            if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Thuế VAT:", formatCurrency(invoice.getTaxAmount()), normalFont, false);
            }
            addTotalRow(totalsTable, "Tổng thanh toán:", formatCurrency(invoice.getTotalAmount()), totalFont, true);

            document.add(totalsTable);

            // 5. Footer / Notes
            document.add(new Paragraph(" "));
            Paragraph thankYou = new Paragraph("Cảm ơn Quý khách đã tin tưởng và mua hàng!", thankYouFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            document.add(thankYou);

            Paragraph notice = new Paragraph("Hóa đơn điện tử này được tạo tự động bởi hệ thống và không cần chữ ký tay.", smallFont);
            notice.setAlignment(Element.ALIGN_CENTER);
            document.add(notice);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi tạo file PDF cho hóa đơn #{}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Không thể tạo file PDF cho hóa đơn #" + invoice.getInvoiceNumber(), e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font, boolean isTotal) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(4);
        labelCell.setBorder(isTotal ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.NO_BORDER);
        if (isTotal) {
            labelCell.setBorderColor(Color.LIGHT_GRAY);
        }

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        valueCell.setBorder(isTotal ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.NO_BORDER);
        if (isTotal) {
            valueCell.setBorderColor(Color.LIGHT_GRAY);
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return CURRENCY_FORMAT.format(amount);
    }
}
