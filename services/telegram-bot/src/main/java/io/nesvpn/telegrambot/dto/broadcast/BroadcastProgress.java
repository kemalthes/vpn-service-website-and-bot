package io.nesvpn.telegrambot.dto.broadcast;

import io.nesvpn.telegrambot.enums.BroadcastCampaignSource;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;

public record BroadcastProgress(
        Long campaignId,
        BroadcastCampaignSource source,
        BroadcastCampaignStatus status,
        Integer totalRecipients,
        Long sentCount,
        Long failedCount,
        Long pendingCount
) {
}
