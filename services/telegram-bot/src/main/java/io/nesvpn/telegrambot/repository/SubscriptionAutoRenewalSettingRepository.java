package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionAutoRenewalSettingRepository extends JpaRepository<SubscriptionAutoRenewalSetting, UUID> {

    @Query(value = """
            insert into subscription_auto_renewal_settings (user_id, enabled, created_at, updated_at)
            values (:userId, true, now(), now())
            on conflict (user_id)
            do update set enabled = not subscription_auto_renewal_settings.enabled,
                          updated_at = now()
            returning enabled
            """, nativeQuery = true)
    Boolean toggleEnabled(@Param("userId") UUID userId);
}
