package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.SubscriptionExpirationNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubscriptionExpirationNotificationRepository extends JpaRepository<SubscriptionExpirationNotification, Long> {

    List<SubscriptionExpirationNotification> findAllByTokenIdIn(Collection<Long> tokenIds);
}
