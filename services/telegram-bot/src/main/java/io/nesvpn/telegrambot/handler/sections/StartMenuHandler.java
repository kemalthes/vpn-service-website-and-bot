package io.nesvpn.telegrambot.handler.sections;

import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.BroadcastService;
import io.nesvpn.telegrambot.services.ReferralService;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.UserService;
import io.nesvpn.telegrambot.util.DisplayTelegramUsername;
import io.nesvpn.telegrambot.util.Formatter;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import io.nesvpn.telegrambot.util.TextFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StartMenuHandler {

    private final VpnBot vpnBot;
    private final UserService userService;
    private final ReferralService referralService;
    private final TelegramUserService telegramUserService;
    private final BroadcastService broadcastService;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;

    public StartMenuHandler(
            @Lazy VpnBot vpnBot,
            UserService userService,
            ReferralService referralService,
            TelegramUserService telegramUserService,
            BroadcastService broadcastService,
            TextFactory textFactory,
            KeyboardFactory keyboardFactory,
            TelegramMessageSender sender
    ) {
        this.vpnBot = vpnBot;
        this.userService = userService;
        this.referralService = referralService;
        this.telegramUserService = telegramUserService;
        this.broadcastService = broadcastService;
        this.textFactory = textFactory;
        this.keyboardFactory = keyboardFactory;
        this.sender = sender;
    }

    public void handleStart(Message message) {
        String text = message.getText();
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        boolean isNewUser = !userService.existsByTgId(userId);
        User user = userService.findOrCreateByTgId(userId);
        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.START);

        if (isNewUser && text != null && text.length() > 7) {
            String payload = text.substring(7);
            User referrer = userService.findByReferralCode(payload);
            if (referrer != null && !referrer.getTgId().equals(userId)) {
                referralService.linkReferral(user, referrer);

                String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());
                sender.sendMessage(
                        referrer.getTgId(),
                        textFactory.newReferralText(displayName, user.getTgId()),
                        null,
                        "HTML"
                );
            }
        }

        showStart(chatId, null, user);
    }

    public void handleProfile(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.PROFILE);
        User user = userService.findOrCreateByTgId(userId);

        showProfile(chatId, null, user);
    }

    public void handleAboutService(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.INFO);

        showAboutService(chatId, null);
    }

    public void handleReferrals(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.REFERRALS);

        User user = userService.findOrCreateByTgId(userId);
        showReferrals(chatId, null, user);
    }

    public void handleInstructions(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.INSTRUCTIONS);

        User user = userService.findOrCreateByTgId(userId);
        showInstructions(chatId, null, user);
    }

    public void showStart(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.START, BotState.START);

        String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.startText(displayName),
                keyboardFactory.getMainMenuInline(broadcastService.isAdmin(user.getTgId())),
                "HTML"
        );
    }

    public void showProfile(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();

        telegramUserService.updateState(userId, BotState.PROFILE, BotState.START);

        String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());
        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();
        String createdAt = user.getCreatedAt() != null ? Formatter.formatMoscow(user.getCreatedAt()) : "Не указано";

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.profileText(
                        displayName,
                        user.getBalance(),
                        userService.getReferralsCount(user.getTgId()),
                        createdAt,
                        referralLink
                ),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }

    public void showReferrals(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.REFERRALS, BotState.START);
        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();
        List<User> referrals = userService.getReferralsByReferrer(userId);

        record ReferralStat(User user, BigDecimal earnings) {
        }

        BigDecimal totalEarnings = BigDecimal.ZERO;
        List<ReferralStat> stats = new ArrayList<>();
        for (User referral : referrals) {
            BigDecimal earnings = referralService.getReferralEarnings(user.getId(), referral.getId());
            BigDecimal safeEarnings = earnings != null ? earnings : BigDecimal.ZERO;

            totalEarnings = totalEarnings.add(safeEarnings);
            stats.add(new ReferralStat(referral, safeEarnings));
        }

        stats.sort((a, b) -> b.earnings().compareTo(a.earnings()));
        StringBuilder referralsList = new StringBuilder();
        int referralsToShow = 15;
        int count = 0;
        for (ReferralStat stat : stats) {
            if (count >= referralsToShow) {
                break;
            }
            User referral = stat.user();
            String referralUsername = DisplayTelegramUsername.getDisplayName(vpnBot, referral.getTgId());
            String refInfo = String.format(
                    "%d) %s (id: %d) принес %.2f₽",
                    count + 1,
                    referralUsername != null ? referralUsername : "no_username",
                    referral.getTgId(),
                    stat.earnings()
            );
            referralsList.append(refInfo).append("\n");
            count++;
        }
        if (referrals.size() > referralsToShow) {
            referralsList.append(String.format("... и еще %d", referrals.size() - referralsToShow));
        }

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.referralsText(referralLink, referrals.size(), totalEarnings, referralsList.toString()),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }

    public void showInstructions(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.INSTRUCTIONS, BotState.SUBSCRIPTIONS);

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.instructionsText(),
                keyboardFactory.getInstructionsMenu(),
                "HTML"
        );
    }

    public void showAndroidInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_ANDROID, BotState.INSTRUCTIONS);
        sender.editMessage(chatId, messageId, textFactory.androidInstructionText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showIosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_IOS, BotState.INSTRUCTIONS);
        sender.editMessage(chatId, messageId, textFactory.iosInstructionText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showWindowsInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_WINDOWS, BotState.INSTRUCTIONS);
        sender.editMessage(chatId, messageId, textFactory.windowsInstructionText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showMacosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_MACOS, BotState.INSTRUCTIONS);
        sender.editMessage(chatId, messageId, textFactory.macosInstructionText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showAboutService(Long chatId, Integer messageId) {
        telegramUserService.updateState(chatId, BotState.INFO, BotState.START);
        sender.editOrSendMessage(chatId, messageId, textFactory.aboutServiceText(), keyboardFactory.getInfoButton(), "HTML");
    }

}
