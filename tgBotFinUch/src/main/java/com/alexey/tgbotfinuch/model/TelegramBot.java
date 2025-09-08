package com.alexey.tgbotfinuch.model;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TelegramBot extends TelegramLongPollingBot {
    private Map<Long, Double> userBalances = new HashMap<>();
    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, List<String>> userDebtors = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendStartMessage(chatId);
            } else if (messageText.equals("/help")) {
                sendHelpMessage(chatId);
            } else {
                handleUserInput(chatId, messageText);
            }
        }
        else if( update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }
    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "ПОЛУЧИЛ":
                userStates.put(chatId, "AWAITING_INCOME_AMOUNT");
                SendMessage inMessage = new SendMessage();
                inMessage.setChatId(chatId);
                inMessage.setText("Enter the amount you earned:");
                try {
                    execute(inMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case "ПОТРАТИЛ":
                userStates.put(chatId, "AWAITING_EXPENSE_AMOUNT");
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Enter the amount you have spent:");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;

            case "ДОЛЖНИК":
                userStates.put(chatId, "AWAITING_DEBTOR");
                SendMessage inMessage1 = new SendMessage();
                inMessage1.setChatId(chatId);
                inMessage1.setText("Enter the name, date and how much he borrow:");
                try {
                    execute(inMessage1);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;

            case "СВОДКА":
                handleSummary(chatId);
                break;
        }   
        
    }
    private void handleSummary(long chatId) {
        double currentBalance = userBalances.getOrDefault(chatId, 0.0);

        String formattedBalance = String.format("%.2f", currentBalance);
        List<String> debtorsList = userDebtors.getOrDefault(chatId, new ArrayList<>());

        String debtorInfo = debtorsList.isEmpty() ?
                "No debtors" :
                String.join("\n", debtorsList);


        SendMessage summaryMessage = new SendMessage();
        summaryMessage.setChatId(chatId);
        summaryMessage.setText("Balance\n\n" +
                "Current balance: " + formattedBalance + " $." + "Your debtors: " + debtorInfo);
        try {
            execute(summaryMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void handleUserInput(long chatId, String inputText) {
        if ("AWAITING_INCOME_AMOUNT".equals(userStates.get(chatId))) {
            try {
                double amount = Double.parseDouble(inputText);
                double currentBalance = userBalances.getOrDefault(chatId, 0.0);

                userBalances.put(chatId, currentBalance + amount);
                userStates.remove(chatId);

                SendMessage confirmation = new SendMessage();
                confirmation.setChatId(chatId);
                confirmation.setText("Added: " + amount + "$" +
                        "\nCurrent balance: " + userBalances.get(chatId) + "$");
                sendHelpMessage(chatId);
                execute(confirmation);
            } catch (NumberFormatException e) {
                SendMessage error = new SendMessage();
                error.setChatId(chatId);
                error.setText("Please enter the correct number");

                try {
                    execute(error);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        else if ("AWAITING_EXPENSE_AMOUNT".equals(userStates.get(chatId))) {
            try {
                double amount = Double.parseDouble(inputText);
                double currentBalance = userBalances.getOrDefault(chatId, 0.0);

                userBalances.put(chatId, currentBalance - amount);

                userStates.remove(chatId);

                SendMessage confirmation = new SendMessage();
                confirmation.setChatId(chatId);
                confirmation.setText("Reduced: " + amount + "$" +
                        "\nCurrent balance: " + userBalances.get(chatId) + "$");

                sendHelpMessage(chatId);

                execute(confirmation);

            } catch (NumberFormatException e) {

                SendMessage error = new SendMessage();
                error.setChatId(chatId);
                error.setText("Please enter the correct number");

                try {
                    execute(error);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        else if ("AWAITING_DEBTOR".equals(userStates.get(chatId))) {
            try {
                String debtorInfo = inputText.trim();

                // Получаем текущий список должников или создаем новый
                List<String> currentDebtors = userDebtors.getOrDefault(chatId, new ArrayList<>());

                // Добавляем нового должника в список
                currentDebtors.add(debtorInfo);

                // Сохраняем обновленный список
                userDebtors.put(chatId, currentDebtors);

                userStates.remove(chatId);

                SendMessage confirmation = new SendMessage();
                confirmation.setChatId(chatId);
                confirmation.setText("Your Debtor: " + debtorInfo);

                sendHelpMessage(chatId);

                execute(confirmation);

            } catch (NumberFormatException e) {
                SendMessage error = new SendMessage();
                error.setChatId(chatId);
                error.setText("Please enter the correct number");

                try {
                    execute(error);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    

    private void sendStartMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Hi, this bot will help you keep track of your funds. Click on -> /help to learn more");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendHelpMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Click on one of the buttons below and enter a number");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();


        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Earned");
        inlineKeyboardButton1.setCallbackData("ПОЛУЧИЛ");

        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton2.setText("Spent");
        inlineKeyboardButton2.setCallbackData("ПОТРАТИЛ");

        rowInline1.add(inlineKeyboardButton1);
        rowInline1.add(inlineKeyboardButton2);


        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton3 = new InlineKeyboardButton();
        inlineKeyboardButton3.setText("balance");
        inlineKeyboardButton3.setCallbackData("СВОДКА");

        InlineKeyboardButton inlineKeyboardButton4 = new InlineKeyboardButton();
        inlineKeyboardButton4.setText("Debtor");
        inlineKeyboardButton4.setCallbackData("ДОЛЖНИК");


        rowInline2.add(inlineKeyboardButton3);
        rowInline2.add(inlineKeyboardButton4);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String getBotUsername() {
        return "@AgregatorPriceForYou_bot";
    }

    @Override
    public String getBotToken() {
        return "8437355068:AAHnzXW8GhL_zzT12LQTM1-ubEB-JYKDKY0";
    }
}
