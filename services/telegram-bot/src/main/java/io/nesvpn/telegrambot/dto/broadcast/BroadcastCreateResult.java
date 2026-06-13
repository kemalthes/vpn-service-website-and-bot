package io.nesvpn.telegrambot.dto.broadcast;

import io.nesvpn.telegrambot.model.BroadcastCampaign;

public record BroadcastCreateResult(
        BroadcastCreateStatus status,
        BroadcastCampaign campaign,
        BroadcastCampaign activeCampaign
) {
    public static BroadcastCreateResult created(BroadcastCampaign campaign) {
        return new BroadcastCreateResult(BroadcastCreateStatus.CREATED, campaign, null);
    }

    public static BroadcastCreateResult activeExists(BroadcastCampaign activeCampaign) {
        return new BroadcastCreateResult(BroadcastCreateStatus.ACTIVE_EXISTS, null, activeCampaign);
    }

    public static BroadcastCreateResult duplicate(BroadcastCampaign campaign) {
        return new BroadcastCreateResult(BroadcastCreateStatus.DUPLICATE, campaign, null);
    }

    public static BroadcastCreateResult ignored() {
        return new BroadcastCreateResult(BroadcastCreateStatus.IGNORED, null, null);
    }
}
