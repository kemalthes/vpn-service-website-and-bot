package io.nesvpn.telegrambot.dto.broadcast;

public interface BroadcastProgressProjection {
    Long getCampaignId();

    String getSource();

    String getStatus();

    Integer getTotalRecipients();

    Long getSentCount();

    Long getFailedCount();

    Long getPendingCount();
}
