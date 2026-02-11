package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.TransactionType;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.repository.UserRepository;
import io.nesvpn.telegrambot.services.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferralService {

    private final UserRepository userRepository;
    private final BalanceService balanceService;

    public String generateReferralCode(Long tgId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(tgId).getBytes());
    }

    public void linkReferral(User user, User referrer) {
        if (user.getReferredBy() != null) {
            return;
        }

        if (user.getTgId().equals(referrer.getTgId())) {
            return;
        }

        user.setReferredBy(referrer.getTgId());
        userRepository.save(user);

        BigDecimal bonus = BigDecimal.valueOf(50.00);
        balanceService.addBalance(user.getId(), bonus, TransactionType.REFERRAL_BONUS, "Бонус за регистрацию по реферальной ссылке");
    }

    @Transactional(readOnly = true)
    public BigDecimal getReferralEarnings(UUID referrerId,UUID referralId) {
        User referral = userRepository.findById(referralId).orElse(null);

        if (referral == null) {
            return BigDecimal.ZERO;
        }

        return balanceService.getReferralEarnings(referrerId, referralId);
    }
}
