package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.UserRepository;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public byte[] generatePdfReport(Long userId, TransactionFilterDto filter) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            userRepository.findById(userId).orElseThrow();

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);


            String fontPath = Objects.requireNonNull(getClass().getClassLoader().getResource("fonts/arial.ttf")).getPath();
            PdfFont font = PdfFontFactory.createFont(fontPath, "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            Paragraph title = new Paragraph("Отчет по транзакциям")
                    .setFont(font)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            String period = String.format("Период: %s - %s",
                    filter.getFromDate(), filter.getToDate());
            document.add(new Paragraph(period).setFont(font));

            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 20, 25, 20}))
                    .useAllAvailableWidth();

            table.addHeaderCell(new Cell().add(new Paragraph("Дата").setFont(font)));
            table.addHeaderCell(new Cell().add(new Paragraph("Категория").setFont(font)));
            table.addHeaderCell(new Cell().add(new Paragraph("Тип").setFont(font)));
            table.addHeaderCell(new Cell().add(new Paragraph("Описание").setFont(font)));
            table.addHeaderCell(new Cell().add(new Paragraph("Сумма").setFont(font)));

            // Данные
            Page<TransactionDto> transactions = transactionService.getTransactions(userId, filter);
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (TransactionDto tx : transactions.getContent()) {
                table.addCell(new Cell().add(new Paragraph(tx.getTransactionDate().toString()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(tx.getCategoryName() != null ? tx.getCategoryName() : "-").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        tx.getType() == TransactionType.INCOME ? "Доход" : "Расход").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(tx.getDescription() != null ? tx.getDescription() : "-").setFont(font)));
                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f %s", tx.getAmount(), tx.getAccountCurrency())).setFont(font)));

                if (tx.getType() == TransactionType.INCOME) {
                    totalIncome = totalIncome.add(tx.getAmount());
                } else {
                    totalExpense = totalExpense.add(tx.getAmount());
                }
            }

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(String.format("Всего доходов: %.2f", totalIncome)).setFont(font));
            document.add(new Paragraph(String.format("Всего расходов: %.2f", totalExpense)).setFont(font));
            document.add(new Paragraph(String.format("Баланс: %.2f", totalIncome.subtract(totalExpense))).setFont(font));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Ошибка генерации PDF отчета", e);
        }
    }

    public byte[] generateExcelReport(Long userId, TransactionFilterDto filter) {
        try (Workbook workbook = new XSSFWorkbook()) {
            userRepository.findById(userId).orElseThrow();

            Sheet sheet = workbook.createSheet("Транзакции");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Дата", "Категория", "Тип", "Описание", "Сумма", "Валюта"};
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            Page<TransactionDto> transactions = transactionService.getTransactions(userId, filter);
            int rowNum = 1;
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (TransactionDto tx : transactions.getContent()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(tx.getTransactionDate().toString());
                row.createCell(1).setCellValue(tx.getCategoryName() != null ? tx.getCategoryName() : "-");
                row.createCell(2).setCellValue(tx.getType() == TransactionType.INCOME ? "Доход" : "Расход");
                row.createCell(3).setCellValue(tx.getDescription() != null ? tx.getDescription() : "-");
                row.createCell(4).setCellValue(tx.getAmount().doubleValue());
                row.createCell(5).setCellValue(tx.getAccountCurrency().toString());

                if (tx.getType() == TransactionType.INCOME) {
                    totalIncome = totalIncome.add(tx.getAmount());
                } else {
                    totalExpense = totalExpense.add(tx.getAmount());
                }
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(3).setCellValue("ИТОГО:");
            totalRow.createCell(4).setCellValue(totalIncome.subtract(totalExpense).doubleValue());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new RuntimeException("Ошибка генерации Excel отчета", e);
        }
    }
}