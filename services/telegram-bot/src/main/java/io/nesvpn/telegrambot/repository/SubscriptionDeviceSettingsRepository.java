package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.SubscriptionDeviceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionDeviceSettingsRepository extends JpaRepository<SubscriptionDeviceSettings, Short> {
}
