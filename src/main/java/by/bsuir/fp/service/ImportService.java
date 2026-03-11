package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.ImportResult;
import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.exception.AccountNotFoundException;
import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.AccountRepository;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public ImportResult importFromCsv(Long userId, Long accountId, MultipartFile file) {
        List<TransactionDto> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

        if (!account.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому счету");
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1 && line.toLowerCase().contains("date")) {
                    continue;
                }

                try {
                    TransactionDto dto = parseCsvLine(line, accountId, dateFormatter);
                    TransactionDto saved = transactionService.createTransaction(userId, dto);
                    imported.add(saved);
                } catch (Exception e) {
                    errors.add("Строка " + lineNumber + ": " + e.getMessage());
                    log.error("Ошибка импорта строки {}: {}", lineNumber, line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения файла: " + e.getMessage());
        }

        return ImportResult.builder()
                .imported(imported)
                .errors(errors)
                .totalCount(lineNumber - 1)
                .successCount(imported.size())
                .errorCount(errors.size())
                .build();
    }

    private TransactionDto parseCsvLine(String line, Long accountId, DateTimeFormatter dateFormatter) {
        String[] fields = line.split(",");

        if (fields.length < 3) {
            throw new IllegalArgumentException("Недостаточно полей. Ожидается: дата,описание,сумма");
        }

        String dateStr = fields[0].trim();
        String description = fields[1].trim();
        String amountStr = fields[2].trim();

        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        BigDecimal amount = new BigDecimal(amountStr);

        TransactionType type = amount.compareTo(BigDecimal.ZERO) >= 0 ?
                TransactionType.EXPENSE : TransactionType.INCOME;

        amount = amount.abs();

        return TransactionDto.builder()
                .accountId(accountId)
                .amount(amount)
                .transactionDate(date)
                .description(description)
                .type(type)
                .isCategorized(false)
                .build();
    }
}