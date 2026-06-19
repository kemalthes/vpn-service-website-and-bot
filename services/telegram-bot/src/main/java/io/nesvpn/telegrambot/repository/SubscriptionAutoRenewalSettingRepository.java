package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionAutoRenewalSettingRepository extends JpaRepository<SubscriptionAutoRenewalSetting, UUID> {
}
