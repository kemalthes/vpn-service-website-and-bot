package io.nesvpn.telegrambot.dto.broadcast;

import io.nesvpn.telegrambot.enums.BroadcastCampaignSource;

public record BroadcastStats(
        Long campaignId,
        BroadcastCampaignSource source,
        Integer totalRecipients,
        Long sentCount,
        Long failedCount
) {
}
