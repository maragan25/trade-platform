package yes.example.no.controller;

import yes.example.no.dto.TradingSummaryDto;
import yes.example.no.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{accountId}/summary")
    public TradingSummaryDto getTradingSummary(@PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.generateTradingSummary(accountId, startDate, endDate);
    }

    @GetMapping("/{accountId}/monthly")
    public TradingSummaryDto getMonthlyReport(@PathVariable Long accountId,
            @RequestParam int year, @RequestParam int month) {
        return reportService.generateMonthlyReport(accountId, year, month);
    }
}