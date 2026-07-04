package io.nesvpn.telegrambot.handler.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Component
public class TelegramMessageSender {

    private final VpnBot vpnBot;
    private final KeyboardFactory keyboardFactory;

    public TelegramMessageSender(@Lazy VpnBot vpnBot, KeyboardFactory keyboardFactory) {
        this.vpnBot = vpnBot;
        this.keyboardFactory = keyboardFactory;
    }

    public void sendMessage(Long chatId, String text, ReplyKeyboard markup, String parseMode) {
        trySendMessage(chatId, text, markup, parseMode);
    }

    public boolean trySendMessage(Long chatId, String text, ReplyKeyboard markup, String parseMode) {
        return sendMessageWithStatus(chatId, text, markup, parseMode) == TelegramDeliveryStatus.SENT;
    }

    public TelegramDeliveryStatus sendMessageWithStatus(Long chatId, String text, ReplyKeyboard markup, String parseMode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        if (markup != null) {
            message.setReplyMarkup(markup);
        }

        message.setParseMode(parseMode);
        message.disableWebPagePreview();

        try {
            vpnBot.execute(message);
            return TelegramDeliveryStatus.SENT;
        } catch (TelegramApiException e) {
            if (isRecipientUnavailable(e)) {
                log.warn(
                        "Telegram message skipped: recipient unavailable. chatId={}, error={}",
                        chatId,
                        formatTelegramError(e)
                );
                return TelegramDeliveryStatus.RECIPIENT_UNAVAILABLE;
            }
            log.error("Telegram API Exception", e);
            return TelegramDeliveryStatus.FAILED;
        }
    }

    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup markup, String parseMode) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode(parseMode);
            editMessage.disableWebPagePreview();
            if (markup != null) {
                editMessage.setReplyMarkup(markup);
            }
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            if (isMessageNotModified(e)) {
                log.debug("Telegram edit skipped: message is not modified. chatId={}, messageId={}", chatId, messageId);
                return;
            }
            if (isRecipientUnavailable(e)) {
                log.warn(
                        "Telegram edit skipped: recipient unavailable. chatId={}, messageId={}, error={}",
                        chatId,
                        messageId,
                        formatTelegramError(e)
                );
                return;
            }
            log.error("Telegram API Exception", e);
        }
    }

    public void editOrSendMessage(
            Long chatId,
            Integer messageId,
            String text,
            InlineKeyboardMarkup markup,
            String parseMode
    ) {
        tryEditOrSendMessage(chatId, messageId, text, markup, parseMode);
    }

    public boolean tryEditOrSendMessage(
            Long chatId,
            Integer messageId,
            String text,
            InlineKeyboardMarkup markup,
            String parseMode
    ) {
        return editOrSendMessageWithStatus(chatId, messageId, text, markup, parseMode) == TelegramDeliveryStatus.SENT;
    }

    public TelegramDeliveryStatus editOrSendMessageWithStatus(
            Long chatId,
            Integer messageId,
            String text,
            InlineKeyboardMarkup markup,
            String parseMode
    ) {
        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                if (markup != null) {
                    editMessage.setReplyMarkup(markup);
                }
                editMessage.setParseMode(parseMode);
                editMessage.disableWebPagePreview();
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                if (markup != null) {
                    sendMessage.setReplyMarkup(markup);
                }
                sendMessage.setParseMode(parseMode);
                sendMessage.disableWebPagePreview();
                vpnBot.execute(sendMessage);
            }
            return TelegramDeliveryStatus.SENT;
        } catch (TelegramApiException e) {
            if (isMessageNotModified(e)) {
                log.debug("Telegram edit skipped: message is not modified. chatId={}, messageId={}", chatId, messageId);
                return TelegramDeliveryStatus.SENT;
            }
            if (isRecipientUnavailable(e)) {
                log.warn(
                        "Telegram {} skipped: recipient unavailable. chatId={}, messageId={}, error={}",
                        messageId != null ? "edit" : "message",
                        chatId,
                        messageId,
                        formatTelegramError(e)
                );
                return TelegramDeliveryStatus.RECIPIENT_UNAVAILABLE;
            }
            log.error("Telegram API Exception", e);
            return TelegramDeliveryStatus.FAILED;
        }
    }

    public void editMessageCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageCaption editCaption = new EditMessageCaption();
            editCaption.setChatId(chatId.toString());
            editCaption.setMessageId(messageId);
            editCaption.setCaption(caption);
            editCaption.setParseMode("HTML");

            if (keyboard != null) {
                editCaption.setReplyMarkup(keyboard);
            }

            vpnBot.execute(editCaption);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    public void sendError(Long chatId, String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(errorText);
        message.setParseMode("HTML");
        message.disableWebPagePreview();

        try {
            vpnBot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
        try {
            vpnBot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    public void removeReplyKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("\u2060");
        message.setReplyMarkup(keyboardFactory.getRemoveReplyKeyboard());

        try {
            Message sentMessage = vpnBot.execute(message);
            deleteMessage(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Failed to remove reply keyboard", e);
        }
    }

    public void removeInlineKeyboard(Long chatId, Integer messageId) {
        if (messageId == null) {
            return;
        }

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(chatId);
        editMarkup.setMessageId(messageId);

        try {
            vpnBot.execute(editMarkup);
        } catch (TelegramApiException e) {
            log.warn(
                    "Failed to remove inline keyboard: chatId={}, messageId={}, error={}",
                    chatId,
                    messageId,
                    formatTelegramError(e)
            );
        }
    }

    public void showPhotoDirectly(
            Long chatId,
            byte[] qrBytes,
            String caption,
            InlineKeyboardMarkup keyboardMarkup
    ) {
        try {
            String botToken = vpnBot.getBotToken();
            String url = "https://api.telegram.org/bot" + botToken + "/sendPhoto";

            String boundary = "------------------------" + System.currentTimeMillis();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            writePart(outputStream, boundary, "chat_id", chatId.toString());
            writeFilePart(outputStream, boundary, "photo", "qr.png", qrBytes);
            writePart(outputStream, boundary, "caption", caption);
            writePart(outputStream, boundary, "parse_mode", "HTML");

            ObjectMapper mapper = new ObjectMapper();
            String replyMarkupJson = mapper.writeValueAsString(keyboardMarkup);
            writePart(outputStream, boundary, "reply_markup", replyMarkupJson);

            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; charset=utf-8; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray()))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Send photo with Telegram API", e);
        }
    }

    public String formatTelegramError(TelegramApiException e) {
        if (e instanceof TelegramApiRequestException requestException) {
            Integer errorCode = requestException.getErrorCode();
            String apiResponse = requestException.getApiResponse();
            if (apiResponse != null && !apiResponse.isBlank()) {
                return "[" + errorCode + "] " + apiResponse;
            }
        }

        return e.getMessage();
    }

    private boolean isMessageNotModified(TelegramApiException e) {
        if (e instanceof TelegramApiRequestException requestException) {
            String apiResponse = requestException.getApiResponse();
            return Integer.valueOf(400).equals(requestException.getErrorCode())
                    && apiResponse != null
                    && apiResponse.contains("message is not modified");
        }

        return false;
    }

    private boolean isRecipientUnavailable(TelegramApiException e) {
        if (e instanceof TelegramApiRequestException requestException) {
            String apiResponse = requestException.getApiResponse();
            String normalizedResponse = apiResponse != null ? apiResponse.toLowerCase(Locale.ROOT) : "";
            Integer errorCode = requestException.getErrorCode();

            return (Integer.valueOf(400).equals(errorCode) && normalizedResponse.contains("chat not found"))
                    || (Integer.valueOf(403).equals(errorCode)
                    && (normalizedResponse.contains("bot was blocked")
                    || normalizedResponse.contains("user is deactivated")
                    || normalizedResponse.contains("bot was kicked")));
        }

        return false;
    }

    private void writePart(ByteArrayOutputStream outputStream, String boundary, String name, String value) {
        try {
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            outputStream.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write multipart data", e);
        }
    }

    private void writeFilePart(
            ByteArrayOutputStream outputStream,
            String boundary,
            String name,
            String filename,
            byte[] data
    ) {
        try {
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(data);
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write multipart file data", e);
        }
    }
}
