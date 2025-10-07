package com.telegram.expense_tracker.telegram;

import com.telegram.expense_tracker.model.Expense;
import com.telegram.expense_tracker.model.UserInfo;
import com.telegram.expense_tracker.service.ExpenseService;
import com.telegram.expense_tracker.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;

    private final UserInfoService userInfoService;

    private final ExpenseService expenseService;

    public TelegramBot(@Value("${telegram.bot.username}") String username, @Value("${telegram.bot.token}") String token, UserInfoService userInfoService, ExpenseService expenseService) {
        super(token);
        this.botUsername = username;
        this.userInfoService = userInfoService;
        this.expenseService = expenseService;
    }

    @Override
    public void onUpdateReceived(Update update) {

        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Chat chat = update.getMessage().getChat();
        boolean isPersonalChat = chat.isUserChat();
        String username = chat.getUserName();
        boolean userCreated = false;

        if (isPersonalChat) {
            UserInfo userInfoByUsername = userInfoService.getUserInfoByUsername(username);
            if (userInfoByUsername == null) {
                userInfoService.createUser(username, chatId);
                userCreated = true;
                sendMessage(chatId, "Welcome " + username + "!");
            }

            if (text.startsWith("/start")) {
                if (userCreated) return;
                userInfoService.createUser(username, chatId);
                sendMessage(chatId, "Welcome to Expense Tracker " + username + "!");
            }

            if (text.startsWith("/add")) {

                String[] parts = text.split("\\s+", 4);
                if (parts.length < 4) {
                    sendMessage(chatId, "❌ Usage: /add <amount> <currency> <remark>");
                    return;
                }

                try {
                    double amount = Double.parseDouble(parts[1]);
                    String currency = parts[2].toUpperCase();
                    String remark = parts[3];

                    expenseService.saveExpense(chatId, currency, amount, remark);

                    // Notify to user
                    sendMessage(chatId, "✅ Added: " + amount + " " + currency + " - " + remark);

                } catch (NumberFormatException e) {
                    sendMessage(chatId, "❌ Invalid amount. Example: /add 50 USD Lunch");
                } catch (Exception e) {
                    sendErrorMessage(e.getMessage(), text);
                }
            }

            if (text.startsWith("/summary")) {
                try {
                    // 1️⃣ Current month
                    LocalDate now = LocalDate.now();
                    LocalDate from = now.withDayOfMonth(1);           // first day of month
                    LocalDate to = now.withDayOfMonth(now.lengthOfMonth()); // last day of month

                    // 2️⃣ Generate the pie chart
                    File chartFile = expenseService.generateCategoryPieChart(username, from, to);

                    // 3️⃣ Send chart image if not null
                    if (chartFile != null) {
                        sendPhoto(chatId, chartFile);
                    }

                    // 4️⃣ Optional: send textual summary
                    String summaryText = expenseService.getSummaryText(username);
                    sendMessage(chatId, summaryText);

                } catch (Exception e) {
                    sendErrorMessage(e.getMessage(), "/summary");
                }
            }

            if (text.startsWith("/clear")) {
                // Clear all expenses except this month
                int result = expenseService.deleteAllExpensesExceptThisMonth(username);

                if (result < 0) {
                    sendMessage(chatId, "This current user did not exist in our system.");
                } else {
                    sendMessage(chatId, "All expenses except this month cleared.\nExpense record deleted: " + result);
                }

            }

            if (text.startsWith("/admin") && username.equals("Lyhor_Hieng")) {

                try {
                    List<UserInfo> allUsers = userInfoService.getAllUsers();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm a");

                    for (UserInfo eachUser : allUsers) {

                        List<Expense> expenseByUsername = expenseService.getExpenseByUsername(eachUser.getUsername());
                        if (expenseByUsername.isEmpty()) continue;

                        StringBuilder sb = new StringBuilder();
                        sb.append("📊 Expense list for ").append(eachUser.getUsername()).append(":\n\n");

                        for (Expense e : expenseByUsername) {

                            Timestamp ts = e.getCreatedOn();
                            String formattedDate = ts.toLocalDateTime().format(formatter);

                            sb.append("• ")
                                    .append(e.getRemark()).append(" - ")
                                    .append(e.getAmount()).append(" (")
                                    .append(formattedDate).append(")\n");
                        }

                        sendMessage(chatId, sb.toString());

                    }
                } catch (Exception e) {
                    sendErrorMessage(e.getMessage(), "/admin");
                }

            }

            if (text.startsWith("/myFund") && username.equals("Lyhor_Hieng")) {
                double amountLeft = 500.0 - expenseService.getSummaryForLyhor();
                sendMessage(chatId, String.format("💵 Hello Lyhor, Your budget left in this month is: %.2f", amountLeft));
            }

        } else {
            sendMessage(chatId, "Our bot only available for personal chat.\nPlease contact our owner @Lyhor_Hieng in order to suggest extra feature.");
        }

    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMsg = new SendMessage(chatId.toString(), text);
        try {
            execute(sendMsg);
        } catch (TelegramApiException e) {
            sendErrorMessage(e.getMessage(), "sendMessage");
        }
    }

    private void sendPhoto(Long chatId, File file) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(file));
        sendPhoto.setChatId(chatId.toString());
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            sendErrorMessage(e.getMessage(), "sendPhoto");
        }
    }

    private void sendErrorMessage(String message, String errorAt) {
        SendMessage sendErrorMessage = new SendMessage("-1002542448425", "⚠️An error occurred" + "\n#ERR_AT: " + errorAt + "\n#MSG: " + message);
        try {
            execute(sendErrorMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

}
