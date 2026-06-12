package io.nesvpn.telegrambot.handler.sections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastCreateResult;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastDeliveryResult;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastProgress;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStats;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import io.nesvpn.telegrambot.model.BroadcastRecipient;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.BroadcastService;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.UserService;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import io.nesvpn.telegrambot.util.TextFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.MessageId;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BroadcastHandler {
    private static final TypeReference<List<MessageEntity>> MESSAGE_ENTITIES_TYPE = new TypeReference<>() {
    };

    private final VpnBot vpnBot;
    private final UserService userService;
    private final TelegramUserService telegramUserService;
    private final BroadcastService broadcastService;
    private final StartMenuHandler startMenuHandler;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BroadcastHandler(
            @Lazy VpnBot vpnBot,
            UserService userService,
            TelegramUserService telegramUserService,
            BroadcastService broadcastService,
            StartMenuHandler startMenuHandler,
            TextFactory textFactory,
            KeyboardFactory keyboardFactory,
            TelegramMessageSender sender
    ) {
        this.vpnBot = vpnBot;
        this.userService = userService;
        this.telegramUserService = telegramUserService;
        this.broadcastService = broadcastService;
        this.startMenuHandler = startMenuHandler;
        this.textFactory = textFactory;
        this.keyboardFactory = keyboardFactory;
        this.sender = sender;
    }

    public void handleAdminBroadcastPost(Message message) {
        Long tgId = message.getFrom().getId();

        try {
            BroadcastCreateResult result = broadcastService.createFromAdminPost(message);
            telegramUserService.setState(tgId, BotState.START);
            handleBroadcastCreateResult(result, message.getChatId());

            User user = userService.findOrCreateByTgId(tgId);
            startMenuHandler.showStart(message.getChatId(), null, user);
        } catch (Exception e) {
            log.error("Failed to create broadcast from admin post", e);
            sender.sendMessage(message.getChatId(), textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
        }
    }

    public void showBroadcast(Long chatId, Integer messageId, User user) {
        if (!broadcastService.isAdmin(user.getTgId())) {
            startMenuHandler.showStart(chatId, messageId, user);
            return;
        }

        telegramUserService.updateState(user.getTgId(), BotState.BROADCAST_AWAITING_POST, BotState.START);
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.broadcastAwaitingPostText(),
                keyboardFactory.getBroadcastAwaitingPostKeyboard(),
                "HTML"
        );
    }

    public void showBroadcastHome(Long chatId, Integer messageId, User user) {
        sender.removeInlineKeyboard(chatId, messageId);
        startMenuHandler.showStart(chatId, null, user);
    }

    public void refreshBroadcastProgress(Long chatId, Integer messageId, User user, Long campaignId) {
        if (!broadcastService.isAdmin(user.getTgId())) {
            startMenuHandler.showStart(chatId, messageId, user);
            return;
        }

        Optional<BroadcastProgress> progress = broadcastService.getProgress(campaignId);
        if (progress.isEmpty()) {
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.dataNotFoundText(),
                    keyboardFactory.getBackButton(),
                    "HTML"
            );
            return;
        }

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.broadcastProgressText(progress.get()),
                keyboardFactory.getBroadcastProgressKeyboard(campaignId),
                "HTML"
        );
    }

    public BroadcastDeliveryResult copyBroadcastMessage(BroadcastRecipient recipient, BroadcastCampaign campaign) {
        if (campaign.getMessageText() != null && !campaign.getMessageText().isBlank()) {
            return sendBroadcastTextMessage(recipient, campaign);
        }

        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setChatId(recipient.getTgId());
        copyMessage.setFromChatId(campaign.getSourceChatId());
        copyMessage.setMessageId(campaign.getSourceMessageId());
        copyMessage.setReplyMarkup(keyboardFactory.getBroadcastHomeKeyboard());

        try {
            MessageId sentMessage = vpnBot.execute(copyMessage);
            return BroadcastDeliveryResult.sent(sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            String errorMessage = sender.formatTelegramError(e);
            log.warn(
                    "Broadcast send failed: campaignId={}, recipientId={}, tgId={}, error={}",
                    campaign.getId(),
                    recipient.getId(),
                    recipient.getTgId(),
                    errorMessage
            );
            return BroadcastDeliveryResult.failed(errorMessage);
        }
    }

    private BroadcastDeliveryResult sendBroadcastTextMessage(BroadcastRecipient recipient, BroadcastCampaign campaign) {
        SendMessage message = new SendMessage();
        message.setChatId(recipient.getTgId());
        message.setText(campaign.getMessageText());
        message.setReplyMarkup(keyboardFactory.getBroadcastHomeKeyboard());
        message.disableWebPagePreview();

        List<MessageEntity> entities = deserializeEntities(campaign);
        if (!entities.isEmpty()) {
            message.setEntities(entities);
        }

        try {
            Message sentMessage = vpnBot.execute(message);
            return BroadcastDeliveryResult.sent(sentMessage.getMessageId().longValue());
        } catch (TelegramApiException e) {
            String errorMessage = sender.formatTelegramError(e);
            log.warn(
                    "Broadcast text send failed: campaignId={}, recipientId={}, tgId={}, error={}",
                    campaign.getId(),
                    recipient.getId(),
                    recipient.getTgId(),
                    errorMessage
            );
            return BroadcastDeliveryResult.failed(errorMessage);
        }
    }

    private List<MessageEntity> deserializeEntities(BroadcastCampaign campaign) {
        String entities = campaign.getMessageEntities();
        if (entities == null || entities.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(entities, MESSAGE_ENTITIES_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize broadcast message entities: campaignId={}", campaign.getId(), e);
            return List.of();
        }
    }

    public void showBroadcastStats(BroadcastStats stats) {
        showBroadcastTextToAdmins(textFactory.broadcastStatsText(stats));
    }

    private void handleBroadcastCreateResult(BroadcastCreateResult result, Long adminChatId) {
        switch (result.status()) {
            case CREATED -> showBroadcastCreatedToAdmins(result.campaign());
            case ACTIVE_EXISTS -> {
                Long activeCampaignId = result.activeCampaign() != null ? result.activeCampaign().getId() : null;
                String text = textFactory.broadcastAlreadyRunningText(activeCampaignId);
                if (adminChatId != null) {
                    sender.sendMessage(adminChatId, text, null, "HTML");
                } else {
                    showBroadcastTextToAdmins(text);
                }
            }
            case DUPLICATE, IGNORED -> {
                if (adminChatId != null) {
                    sender.sendMessage(adminChatId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
                }
            }
        }
    }

    private void showBroadcastCreatedToAdmins(BroadcastCampaign campaign) {
        showBroadcastTextToAdmins(
                textFactory.broadcastCreatedText(
                        campaign.getId(),
                        campaign.getTotalRecipients()
                ),
                keyboardFactory.getBroadcastProgressKeyboard(campaign.getId())
        );
    }

    private void showBroadcastTextToAdmins(String text) {
        showBroadcastTextToAdmins(text, null);
    }

    private void showBroadcastTextToAdmins(String text, InlineKeyboardMarkup keyboard) {
        for (Long adminId : broadcastService.getAdminIds()) {
            sender.trySendMessage(adminId, text, keyboard, "HTML");
        }
    }

}
