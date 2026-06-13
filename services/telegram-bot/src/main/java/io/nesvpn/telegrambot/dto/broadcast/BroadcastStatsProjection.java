package io.nesvpn.telegrambot.dto.broadcast;

public interface BroadcastStatsProjection {
    Long getCampaignId();

    String getSource();

    Integer getTotalRecipients();

    Long getSentCount();

    Long getFailedCount();
}
