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

public class TelegramBot extends TelegramLongPollingBot {
    private Map<Long, Double> userBalances = new HashMap<>(); // chatId -> баланс
    private Map<Long, String> userStates = new HashMap<>(); // chatId -> состояние пользователя

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, есть ли сообщение и текст
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Обрабатываем команду /start
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
                // Устанавливаем состояние "ожидание ввода суммы для пополнения"
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
                // Устанавливаем состояние "ожидание ввода суммы для пополнения"
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

            case "СВОДКА":
                handleSummary(chatId);
                break;
        }   
        
    }
    private void handleSummary(long chatId) {
        // Получаем текущий баланс пользователя
        // getOrDefault возвращает 0.0 если баланс еще не установлен
        double currentBalance = userBalances.getOrDefault(chatId, 0.0);

        // Форматируем баланс для красивого отображения
        String formattedBalance = String.format("%.2f", currentBalance);

        // Создаем сообщение со сводкой
        SendMessage summaryMessage = new SendMessage();
        summaryMessage.setChatId(chatId);
        summaryMessage.setText("Balance\n\n" +
                "Current balance: " + formattedBalance + " $.");
        try {
            execute(summaryMessage); // Не забываем отправить сообщение!
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void handleUserInput(long chatId, String inputText) {
        // Проверяем, ожидает ли бот ввода суммы от этого пользователя
        if ("AWAITING_INCOME_AMOUNT".equals(userStates.get(chatId))) {
            try {
                // Парсим введенное число
                double amount = Double.parseDouble(inputText);

                // Получаем текущий баланс (или 0.0 если его еще нет)
                double currentBalance = userBalances.getOrDefault(chatId, 0.0);

                // Добавляем сумму к балансу
                userBalances.put(chatId, currentBalance + amount);

                // Сбрасываем состояние пользователя
                userStates.remove(chatId);

                // Отправляем подтверждение
                SendMessage confirmation = new SendMessage();
                confirmation.setChatId(chatId);
                confirmation.setText("Added: " + amount + "$" +
                        "\nCurrent balance: " + userBalances.get(chatId) + "$");

                // Показываем меню снова
                sendHelpMessage(chatId);

                execute(confirmation);

            } catch (NumberFormatException e) {
                // Если введено не число
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
        // Здесь можно добавить обработку других состояний
        else if ("AWAITING_EXPENSE_AMOUNT".equals(userStates.get(chatId))) {
            try {
                // Парсим введенное число
                double amount = Double.parseDouble(inputText);

                // Получаем текущий баланс (или 0.0 если его еще нет)
                double currentBalance = userBalances.getOrDefault(chatId, 0.0);

                // Добавляем сумму к балансу
                userBalances.put(chatId, currentBalance - amount);

                // Сбрасываем состояние пользователя
                userStates.remove(chatId);

                // Отправляем подтверждение
                SendMessage confirmation = new SendMessage();
                confirmation.setChatId(chatId);
                confirmation.setText("Reduced: " + amount + "$" +
                        "\nCurrent balance: " + userBalances.get(chatId) + "$");

                // Показываем меню снова
                sendHelpMessage(chatId);

                execute(confirmation);

            } catch (NumberFormatException e) {
                // Если введено не число
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

        // Первый ряд кнопок
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Earned");
        inlineKeyboardButton1.setCallbackData("ПОЛУЧИЛ"); // Убрал лишние кавычки

        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton2.setText("Spent");
        inlineKeyboardButton2.setCallbackData("ПОТРАТИЛ"); // Убрал лишние кавычки

        rowInline1.add(inlineKeyboardButton1);
        rowInline1.add(inlineKeyboardButton2);

        // Второй ряд кнопок
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton3 = new InlineKeyboardButton();
        inlineKeyboardButton3.setText("balance");
        inlineKeyboardButton3.setCallbackData("СВОДКА");

        // Для кнопок с URL callbackData обычно не нужен
        rowInline2.add(inlineKeyboardButton3);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return "YourBotName";
    }

    @Override
    public String getBotToken() {
        return "YourBotToken";
    }
}
