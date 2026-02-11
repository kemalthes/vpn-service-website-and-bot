package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.BotState;
import io.nesvpn.telegrambot.model.TelegramUser;
import io.nesvpn.telegrambot.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    public Optional<TelegramUser> findByTgId(Long tgId) {
        return telegramUserRepository.findByTgId(tgId);
    }

    public TelegramUser findOrCreate(Long tgId) {
        return telegramUserRepository.findByTgId(tgId)
                .orElseGet(() -> create(tgId, BotState.START));
    }


    public boolean existsByTgId(Long tgId) {
        return telegramUserRepository.existsByTgId(tgId);
    }

    public TelegramUser create(Long tgId, BotState state) {
        TelegramUser telegramUser = new TelegramUser();
        telegramUser.setTgId(tgId);
        telegramUser.setState(state.toString());
        telegramUser.setPreviousState(null);
        return telegramUserRepository.save(telegramUser);
    }

    public void updateState(Long tgId, BotState newState, BotState previousState) {
        TelegramUser telegramUser = findOrCreate(tgId);
        telegramUser.setPreviousState(previousState != null ? previousState.toString() : telegramUser.getState());
        telegramUser.setState(newState.toString());
        telegramUserRepository.save(telegramUser);
    }

    public BotState getCurrentState(Long tgId) {
        return telegramUserRepository.findByTgId(tgId)
                .map(tu -> BotState.fromString(tu.getState()))
                .orElse(BotState.START);
    }

    public BotState getPreviousState(Long tgId) {
        return telegramUserRepository.findByTgId(tgId)
                .map(tu -> tu.getPreviousState() != null
                        ? BotState.fromString(tu.getPreviousState())
                        : null)
                .orElse(null);
    }

    public void goToPreviousState(Long tgId) {
        TelegramUser telegramUser = findOrCreate(tgId);

        if (telegramUser.getPreviousState() != null) {
            String temp = telegramUser.getState();
            telegramUser.setState(telegramUser.getPreviousState());
            telegramUser.setPreviousState(temp);
            telegramUserRepository.save(telegramUser);
        }
    }

    public void resetState(Long tgId) {
        updateState(tgId, BotState.START, null);
    }

    public void setState(Long tgId, BotState state) {
        TelegramUser telegramUser = findOrCreate(tgId);

        telegramUser.setPreviousState(BotState.START.toString());
        telegramUser.setState(state.toString());

        telegramUserRepository.save(telegramUser);
    }



    public TelegramUser save(TelegramUser telegramUser) {
        return telegramUserRepository.save(telegramUser);
    }
}
