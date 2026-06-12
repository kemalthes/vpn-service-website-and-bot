package io.nesvpn.telegrambot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastCreateResult;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastProgress;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastProgressProjection;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStats;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStatsProjection;
import io.nesvpn.telegrambot.enums.BroadcastCampaignSource;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import io.nesvpn.telegrambot.enums.BroadcastRecipientStatus;
import io.nesvpn.telegrambot.enums.UserRole;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import io.nesvpn.telegrambot.model.BroadcastRecipient;
import io.nesvpn.telegrambot.repository.BroadcastCampaignRepository;
import io.nesvpn.telegrambot.repository.BroadcastRecipientRepository;
import io.nesvpn.telegrambot.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {
    private static final EnumSet<BroadcastCampaignStatus> ACTIVE_CAMPAIGN_STATUSES = EnumSet.of(
            BroadcastCampaignStatus.PREPARING,
            BroadcastCampaignStatus.PROCESSING
    );

    private final BroadcastCampaignRepository broadcastCampaignRepository;
    private final BroadcastRecipientRepository broadcastRecipientRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService recipientPreparationExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "broadcast-recipient-preparation");
        thread.setDaemon(true);
        return thread;
    });

    public boolean isAdmin(Long tgId) {
        return tgId != null && userRepository.existsByTgIdAndRoleIgnoreCase(tgId, UserRole.ADMIN.getValue());
    }

    public List<Long> getAdminIds() {
        return userRepository.findTgIdsByRole(UserRole.ADMIN.getValue());
    }

    @Transactional(readOnly = true)
    public Optional<BroadcastProgress> getProgress(Long campaignId) {
        return broadcastCampaignRepository.findProgressByCampaignId(campaignId)
                .map(this::toBroadcastProgress);
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
                message.hasText() ? message.getText() : null,
                message.hasText() ? serializeEntities(message.getEntities()) : null,
                message.getFrom().getId()
        );
    }

    @Transactional(readOnly = true)
    public List<BroadcastRecipient> findPendingRecipients(int limit) {
        return broadcastRecipientRepository.findPendingRecipientsForProcessingCampaigns(limit);
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
        return broadcastCampaignRepository.completeReadyCampaigns(limit).stream()
                .map(this::toBroadcastStats)
                .toList();
    }

    @Transactional
    public int deleteOldFinishedCampaignRecipients(LocalDateTime createdBefore) {
        return broadcastRecipientRepository.deleteFinishedCampaignRecipientsCreatedBefore(createdBefore);
    }

    private BroadcastCreateResult createCampaign(
            BroadcastCampaignSource source,
            Long sourceChatId,
            Integer sourceMessageId,
            String messageText,
            String messageEntities,
            Long adminTgId
    ) {
        Optional<BroadcastCampaign> existingCampaign =
                broadcastCampaignRepository.findBySourceChatIdAndSourceMessageId(sourceChatId, sourceMessageId);
        if (existingCampaign.isPresent()) {
            return BroadcastCreateResult.duplicate(existingCampaign.get());
        }

        Optional<BroadcastCampaign> activeCampaign =
                broadcastCampaignRepository.findFirstByStatusInOrderByCreatedAtAsc(ACTIVE_CAMPAIGN_STATUSES);
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
        campaign.setMessageText(messageText);
        campaign.setMessageEntities(messageEntities);
        campaign.setAdminTgId(adminTgId);
        campaign.setStatus(BroadcastCampaignStatus.PREPARING);
        campaign.setTotalRecipients(0);

        campaign = broadcastCampaignRepository.saveAndFlush(campaign);
        scheduleRecipientPreparation(campaign.getId());

        log.info(
                "Broadcast campaign created: campaignId={}, source={}, sourceChatId={}, sourceMessageId={}, status={}",
                campaign.getId(),
                campaign.getSource(),
                campaign.getSourceChatId(),
                campaign.getSourceMessageId(),
                campaign.getStatus()
        );

        return BroadcastCreateResult.created(campaign);
    }

    private String serializeEntities(List<MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(entities);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize broadcast message entities", e);
            return null;
        }
    }

    private void scheduleRecipientPreparation(Long campaignId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            recipientPreparationExecutor.submit(() -> prepareRecipients(campaignId));
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                recipientPreparationExecutor.submit(() -> prepareRecipients(campaignId));
            }
        });
    }

    private void prepareRecipients(Long campaignId) {
        try {
            Integer recipients = transactionTemplate.execute(status -> {
                BroadcastCampaign campaign = broadcastCampaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalStateException("Broadcast campaign not found: " + campaignId));

                int insertedRecipients = broadcastRecipientRepository.insertRecipientsForCampaign(campaignId);
                campaign.setTotalRecipients(insertedRecipients);
                campaign.setStatus(BroadcastCampaignStatus.PROCESSING);
                broadcastCampaignRepository.save(campaign);
                return insertedRecipients;
            });

            log.info(
                    "Broadcast recipients prepared: campaignId={}, recipients={}",
                    campaignId,
                    recipients
            );
        } catch (Exception e) {
            log.error("Broadcast recipients preparation failed: campaignId={}", campaignId, e);
            markCampaignPreparationFailed(campaignId);
        }
    }

    private void markCampaignPreparationFailed(Long campaignId) {
        transactionTemplate.executeWithoutResult(status ->
                broadcastCampaignRepository.findById(campaignId).ifPresent(campaign -> {
                    campaign.setStatus(BroadcastCampaignStatus.FAILED);
                    campaign.setCompletedAt(LocalDateTime.now());
                    broadcastCampaignRepository.save(campaign);
                })
        );
    }

    private BroadcastStats toBroadcastStats(BroadcastStatsProjection projection) {
        BroadcastStats stats = new BroadcastStats(
                projection.getCampaignId(),
                BroadcastCampaignSource.valueOf(projection.getSource()),
                projection.getTotalRecipients(),
                projection.getSentCount(),
                projection.getFailedCount()
        );

        log.info(
                "Broadcast campaign completed: campaignId={}, total={}, sent={}, failed={}",
                stats.campaignId(),
                stats.totalRecipients(),
                stats.sentCount(),
                stats.failedCount()
        );

        return stats;
    }

    private BroadcastProgress toBroadcastProgress(BroadcastProgressProjection projection) {
        return new BroadcastProgress(
                projection.getCampaignId(),
                BroadcastCampaignSource.valueOf(projection.getSource()),
                BroadcastCampaignStatus.valueOf(projection.getStatus()),
                projection.getTotalRecipients(),
                projection.getSentCount(),
                projection.getFailedCount(),
                projection.getPendingCount()
        );
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage;
    }

    @PreDestroy
    public void shutdownRecipientPreparationExecutor() {
        recipientPreparationExecutor.shutdownNow();
    }

}
