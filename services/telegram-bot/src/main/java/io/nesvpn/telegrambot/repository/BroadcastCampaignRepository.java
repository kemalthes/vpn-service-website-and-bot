package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastCampaignRepository extends JpaRepository<BroadcastCampaign, Long> {

    List<BroadcastCampaign> findByStatusOrderByCreatedAtAsc(BroadcastCampaignStatus status, Pageable pageable);

    Optional<BroadcastCampaign> findFirstByStatusOrderByCreatedAtAsc(BroadcastCampaignStatus status);

    Optional<BroadcastCampaign> findBySourceChatIdAndSourceMessageId(Long sourceChatId, Integer sourceMessageId);
}
