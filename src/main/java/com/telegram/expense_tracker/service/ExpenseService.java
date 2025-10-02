package com.telegram.expense_tracker.service;

import com.telegram.expense_tracker.model.Expense;
import com.telegram.expense_tracker.model.UserInfo;
import com.telegram.expense_tracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final UserInfoService userInfoService;
    private final ExpenseRepository expenseRepository;

    public List<Expense> getExpenseByUsername(String username) {
        UserInfo userInfoByUsername = userInfoService.getUserInfoByUsername(username);
        if (userInfoByUsername == null) return new ArrayList<>();
        return expenseRepository.findByUserId(userInfoByUsername.getId());
    }

    public void saveExpense(Long userId, String currency, double amount, String remark) {
        BigDecimal decimalAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        Expense newExpense = new Expense(userId, currency, decimalAmount, remark);
        expenseRepository.save(newExpense);
    }

    public File generateCategoryPieChart(String username, LocalDate from, LocalDate to) throws Exception {
        // 1️⃣ Fetch the user
        UserInfo user = userInfoService.getUserInfoByUsername(username);
        if (user == null) throw new Exception("User not found");

        // 2️⃣ Get expenses in the date range
        List<Expense> expenses = expenseRepository.findByUserId(user.getId()).stream()
                .filter(e -> {
                    LocalDate date = e.getCreatedOn().toLocalDateTime().toLocalDate();
                    return (date.isEqual(from) || date.isAfter(from)) &&
                            (date.isEqual(to) || date.isBefore(to));
                })
                .toList();

        if (expenses.isEmpty()) return null;

        // 3️⃣ Sum by remark
        Map<String, BigDecimal> sumByRemark = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getRemark() == null ? "" : e.getRemark().toLowerCase(), // lowercase key
                        Collectors.mapping(Expense::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // 4️⃣ Build chart
        PieChart chart = new PieChartBuilder()
                .width(1000).height(800)
                .title("Expense Breakdown")
                .build();

        sumByRemark.forEach((k, v) -> chart.addSeries(k, v.doubleValue()));

        // 5️⃣ Save chart to temp file
        File chartFile = File.createTempFile("chart", ".png");
        BitmapEncoder.saveBitmap(chart, chartFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);

        return chartFile;
    }

    public String getSummaryText(String username) throws Exception {
        // 1️⃣ Fetch the user
        UserInfo user = userInfoService.getUserInfoByUsername(username);
        if (user == null) throw new Exception("User not found");

        // 2️⃣ Get all expenses for this user
        List<Expense> expenses = expenseRepository.findByUserId(user.getId());
        if (expenses.isEmpty()) return "📊 You have no expenses recorded yet.";

        // 3️⃣ Group by remark and sum amounts
        Map<String, BigDecimal> sumByRemark = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getRemark() == null ? "" : capitalizeWords(e.getRemark()),
                        Collectors.mapping(Expense::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // 4️⃣ Build summary text
        StringBuilder sb = new StringBuilder("📊 Expense Summary Current Month:\n");
        sumByRemark.forEach((remark, total) ->
                sb.append("- ").append(remark)
                        .append(": ").append(total)
                        .append("\n")
        );

        Map<String, BigDecimal> totalByCurrency = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        totalByCurrency.forEach((currency, total) ->
                sb.append("\nTotal (").append(currency).append("): ").append(total)
        );

        return sb.toString();
    }

    public int deleteAllExpensesExceptThisMonth(String username) {

        UserInfo userInfoByUsername = userInfoService.getUserInfoByUsername(username);
        if (userInfoByUsername == null)  return -1;

        LocalDate now = LocalDate.now();
        return expenseRepository.deleteAllExceptCurrentMonth(userInfoByUsername.getId(), now.getYear(), now.getMonthValue());

    }

    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();

        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

}
