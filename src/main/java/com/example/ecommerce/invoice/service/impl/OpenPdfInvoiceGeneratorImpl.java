package com.example.ecommerce.invoice.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.invoice.service.InvoicePdfGenerator;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
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
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00 VND", symbols);
    }

    @Value("${app.invoice.store-name:E-COMMERCE STORE}")
    private String storeName;

    @Value("${app.invoice.store-address:123 Le Loi, District 1, Ho Chi Minh City}")
    private String storeAddress;

    @Value("${app.invoice.store-phone:+84 28 1234 5678}")
    private String storePhone;

    @Value("${app.invoice.store-email:support@ecommerce.local}")
    private String storeEmail;

    @Override
    public byte[] generateInvoicePdf(InvoiceEntity invoice) {
        if (invoice == null) {
            throw new BadRequestException("Invoice cannot be null for PDF generation.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // 1. Header: Store info + Invoice Title
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell storeCell = new PdfPCell();
            storeCell.setBorder(Rectangle.NO_BORDER);
            storeCell.addElement(new Paragraph(storeName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(41, 128, 185))));
            storeCell.addElement(new Paragraph(storeAddress, smallFont));
            storeCell.addElement(new Paragraph("Phone: " + storePhone + " | Email: " + storeEmail, smallFont));
            headerTable.addCell(storeCell);

            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph title = new Paragraph("SALES INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(title);

            Paragraph invNum = new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), boldFont);
            invNum.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invNum);

            Paragraph issueDate = new Paragraph("Date: " + invoice.getIssueDate().format(DATE_TIME_FORMATTER), smallFont);
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
            billToCell.setBorderColor(Color.LIGHT_GRAY);
            billToCell.setPadding(8);
            billToCell.setBackgroundColor(new Color(245, 247, 250));
            billToCell.addElement(new Paragraph("BILL TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(41, 128, 185))));
            billToCell.addElement(new Paragraph(invoice.getBillingName(), boldFont));
            billToCell.addElement(new Paragraph("Email: " + invoice.getBillingEmail(), normalFont));
            billToCell.addElement(new Paragraph("Phone: " + invoice.getBillingPhone(), normalFont));
            if (invoice.getBillingAddress() != null && !invoice.getBillingAddress().isBlank()) {
                billToCell.addElement(new Paragraph("Address: " + invoice.getBillingAddress(), normalFont));
            }
            infoTable.addCell(billToCell);

            PdfPCell orderMetaCell = new PdfPCell();
            orderMetaCell.setBorder(Rectangle.BOX);
            orderMetaCell.setBorderColor(Color.LIGHT_GRAY);
            orderMetaCell.setPadding(8);
            orderMetaCell.setBackgroundColor(new Color(245, 247, 250));
            orderMetaCell.addElement(new Paragraph("ORDER DETAILS:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(41, 128, 185))));
            orderMetaCell.addElement(new Paragraph("Order ID: #" + (invoice.getOrder() != null ? invoice.getOrder().getId() : "N/A"), normalFont));
            orderMetaCell.addElement(new Paragraph("Status: " + invoice.getStatus().name(), normalFont));
            orderMetaCell.addElement(new Paragraph("Payment Method: " + (invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "N/A"), normalFont));
            if (invoice.getPaymentTransactionRef() != null) {
                orderMetaCell.addElement(new Paragraph("Txn Ref: " + invoice.getPaymentTransactionRef(), smallFont));
            }
            infoTable.addCell(orderMetaCell);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 3. Invoice Items Table
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{8, 42, 12, 18, 20});
            itemsTable.setHeaderRows(1);

            // Table Header
            Color headerBg = new Color(52, 73, 94);
            addHeaderCell(itemsTable, "#", headerFont, headerBg, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Product Description", headerFont, headerBg, Element.ALIGN_LEFT);
            addHeaderCell(itemsTable, "Qty", headerFont, headerBg, Element.ALIGN_CENTER);
            addHeaderCell(itemsTable, "Unit Price", headerFont, headerBg, Element.ALIGN_RIGHT);
            addHeaderCell(itemsTable, "Amount", headerFont, headerBg, Element.ALIGN_RIGHT);

            // Table Rows
            int index = 1;
            for (InvoiceItemEntity item : invoice.getItems()) {
                Color rowBg = (index % 2 == 0) ? new Color(248, 249, 250) : Color.WHITE;
                addBodyCell(itemsTable, String.valueOf(index++), normalFont, rowBg, Element.ALIGN_CENTER);
                addBodyCell(itemsTable, item.getProductName(), normalFont, rowBg, Element.ALIGN_LEFT);
                addBodyCell(itemsTable, String.valueOf(item.getQuantity()), normalFont, rowBg, Element.ALIGN_CENTER);
                addBodyCell(itemsTable, formatCurrency(item.getUnitPrice()), normalFont, rowBg, Element.ALIGN_RIGHT);
                addBodyCell(itemsTable, formatCurrency(item.getSubtotal()), normalFont, rowBg, Element.ALIGN_RIGHT);
            }

            document.add(itemsTable);
            document.add(new Paragraph(" "));

            // 4. Totals Summary Table
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(45);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{50, 50});

            addTotalRow(totalsTable, "Subtotal:", formatCurrency(invoice.getSubtotal()), normalFont, false);
            if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Discount:", "-" + formatCurrency(invoice.getDiscountAmount()), normalFont, false);
            }
            if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Tax:", formatCurrency(invoice.getTaxAmount()), normalFont, false);
            }
            addTotalRow(totalsTable, "Total Amount:", formatCurrency(invoice.getTotalAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(41, 128, 185)), true);

            document.add(totalsTable);

            // 5. Footer / Notes
            document.add(new Paragraph(" "));
            Paragraph thankYou = new Paragraph("Thank you for your purchase!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY));
            thankYou.setAlignment(Element.ALIGN_CENTER);
            document.add(thankYou);

            Paragraph notice = new Paragraph("This is a computer-generated invoice and requires no signature.", smallFont);
            notice.setAlignment(Element.ALIGN_CENTER);
            document.add(notice);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating invoice PDF for invoice #{}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to generate PDF for invoice #" + invoice.getInvoiceNumber(), e);
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
            return "0.00 VND";
        }
        return CURRENCY_FORMAT.format(amount);
    }
}
