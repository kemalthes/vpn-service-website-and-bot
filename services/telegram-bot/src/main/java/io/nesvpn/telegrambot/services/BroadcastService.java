package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.BroadcastCampaignSource;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import io.nesvpn.telegrambot.enums.BroadcastRecipientStatus;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import io.nesvpn.telegrambot.model.BroadcastRecipient;
import io.nesvpn.telegrambot.repository.BroadcastCampaignRepository;
import io.nesvpn.telegrambot.repository.BroadcastRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {
    private static final String SOURCE_CHANNEL_ID = "3000857338";
    private static final Set<Long> ADMIN_IDS = Set.of(5311630558L, 7378974496L);

    private final BroadcastCampaignRepository broadcastCampaignRepository;
    private final BroadcastRecipientRepository broadcastRecipientRepository;

    public boolean isAdmin(Long tgId) {
        return tgId != null && ADMIN_IDS.contains(tgId);
    }

    public List<Long> getAdminIds() {
        return ADMIN_IDS.stream().toList();
    }

    public boolean isSourceChannel(Message message) {
        if (message == null || message.getChatId() == null) {
            return false;
        }

        String chatId = String.valueOf(Math.abs(message.getChatId()));
        return chatId.equals(SOURCE_CHANNEL_ID) || chatId.endsWith(SOURCE_CHANNEL_ID);
    }

    @Transactional
    public BroadcastCreateResult createFromChannelPost(Message message) {
        if (!isSourceChannel(message)) {
            return BroadcastCreateResult.ignored();
        }

        return createCampaign(
                BroadcastCampaignSource.CHANNEL,
                message.getChatId(),
                message.getMessageId(),
                null
        );
    }

    @Transactional
    public BroadcastCreateResult createFromAdminPost(Message message) {
        if (message == null || message.getFrom() == null || !isAdmin(message.getFrom().getId())) {
            return BroadcastCreateResult.ignored();
        }

        return createCampaign(
                BroadcastCampaignSource.ADMIN,
                message.getChatId(),
                message.getMessageId(),
                message.getFrom().getId()
        );
    }

    @Transactional(readOnly = true)
    public List<BroadcastRecipient> findPendingRecipients(int limit) {
        return broadcastRecipientRepository.findByStatusOrderByIdAsc(
                BroadcastRecipientStatus.PENDING,
                PageRequest.of(0, limit)
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, BroadcastCampaign> getCampaignsByIds(List<Long> campaignIds) {
        return broadcastCampaignRepository.findAllById(campaignIds).stream()
                .collect(Collectors.toMap(BroadcastCampaign::getId, Function.identity()));
    }

    @Transactional
    public void markRecipientSent(BroadcastRecipient recipient, Long sentMessageId) {
        recipient.setStatus(BroadcastRecipientStatus.SENT);
        recipient.setSentMessageId(sentMessageId);
        recipient.setErrorMessage(null);
        recipient.setSentAt(LocalDateTime.now());
        recipient.setUpdatedAt(LocalDateTime.now());
        broadcastRecipientRepository.save(recipient);
    }

    @Transactional
    public void markRecipientFailed(BroadcastRecipient recipient, String errorMessage) {
        recipient.setStatus(BroadcastRecipientStatus.FAILED);
        recipient.setErrorMessage(trimError(errorMessage));
        recipient.setUpdatedAt(LocalDateTime.now());
        broadcastRecipientRepository.save(recipient);
    }

    @Transactional
    public List<BroadcastStats> completeReadyCampaigns(int limit) {
        List<BroadcastCampaign> campaigns = broadcastCampaignRepository.findByStatusOrderByCreatedAtAsc(
                BroadcastCampaignStatus.PROCESSING,
                PageRequest.of(0, limit)
        );

        return campaigns.stream()
                .filter(campaign -> !broadcastRecipientRepository.existsByCampaignIdAndStatus(
                        campaign.getId(),
                        BroadcastRecipientStatus.PENDING
                ))
                .map(this::completeCampaign)
                .toList();
    }

    private BroadcastCreateResult createCampaign(
            BroadcastCampaignSource source,
            Long sourceChatId,
            Integer sourceMessageId,
            Long adminTgId
    ) {
        Optional<BroadcastCampaign> existingCampaign =
                broadcastCampaignRepository.findBySourceChatIdAndSourceMessageId(sourceChatId, sourceMessageId);
        if (existingCampaign.isPresent()) {
            return BroadcastCreateResult.duplicate(existingCampaign.get());
        }

        Optional<BroadcastCampaign> activeCampaign =
                broadcastCampaignRepository.findFirstByStatusOrderByCreatedAtAsc(BroadcastCampaignStatus.PROCESSING);
        if (activeCampaign.isPresent()) {
            log.info(
                    "Broadcast campaign skipped: activeCampaignId={} newSource={} sourceChatId={} sourceMessageId={}",
                    activeCampaign.get().getId(),
                    source,
                    sourceChatId,
                    sourceMessageId
            );
            return BroadcastCreateResult.activeExists(activeCampaign.get());
        }

        BroadcastCampaign campaign = new BroadcastCampaign();
        campaign.setSource(source);
        campaign.setSourceChatId(sourceChatId);
        campaign.setSourceMessageId(sourceMessageId);
        campaign.setAdminTgId(adminTgId);
        campaign.setStatus(BroadcastCampaignStatus.PROCESSING);
        campaign.setTotalRecipients(0);

        campaign = broadcastCampaignRepository.saveAndFlush(campaign);
        int recipients = broadcastRecipientRepository.insertRecipientsForCampaign(campaign.getId());
        campaign.setTotalRecipients(recipients);
        campaign = broadcastCampaignRepository.save(campaign);

        log.info(
                "Broadcast campaign created: campaignId={}, source={}, sourceChatId={}, sourceMessageId={}, recipients={}",
                campaign.getId(),
                campaign.getSource(),
                campaign.getSourceChatId(),
                campaign.getSourceMessageId(),
                campaign.getTotalRecipients()
        );

        return BroadcastCreateResult.created(campaign);
    }

    private BroadcastStats completeCampaign(BroadcastCampaign campaign) {
        long sentCount = broadcastRecipientRepository.countByCampaignIdAndStatus(
                campaign.getId(),
                BroadcastRecipientStatus.SENT
        );
        long failedCount = broadcastRecipientRepository.countByCampaignIdAndStatus(
                campaign.getId(),
                BroadcastRecipientStatus.FAILED
        );

        campaign.setStatus(BroadcastCampaignStatus.COMPLETED);
        campaign.setCompletedAt(LocalDateTime.now());
        broadcastCampaignRepository.save(campaign);

        log.info(
                "Broadcast campaign completed: campaignId={}, total={}, sent={}, failed={}",
                campaign.getId(),
                campaign.getTotalRecipients(),
                sentCount,
                failedCount
        );

        return new BroadcastStats(
                campaign.getId(),
                campaign.getSource(),
                campaign.getTotalRecipients(),
                sentCount,
                failedCount
        );
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage;
    }

    public record BroadcastStats(
            Long campaignId,
            BroadcastCampaignSource source,
            Integer totalRecipients,
            Long sentCount,
            Long failedCount
    ) {
    }

    public enum BroadcastCreateStatus {
        CREATED,
        ACTIVE_EXISTS,
        DUPLICATE,
        IGNORED
    }

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
}
