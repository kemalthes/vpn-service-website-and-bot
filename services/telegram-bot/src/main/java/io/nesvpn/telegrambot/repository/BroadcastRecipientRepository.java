package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.enums.BroadcastRecipientStatus;
import io.nesvpn.telegrambot.model.BroadcastRecipient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastRecipientRepository extends JpaRepository<BroadcastRecipient, Long> {

    List<BroadcastRecipient> findByStatusOrderByIdAsc(BroadcastRecipientStatus status, Pageable pageable);

    boolean existsByCampaignIdAndStatus(Long campaignId, BroadcastRecipientStatus status);

    long countByCampaignIdAndStatus(Long campaignId, BroadcastRecipientStatus status);

    @Modifying
    @Query(value = """
            INSERT INTO broadcast_recipients (
                campaign_id,
                user_id,
                tg_id,
                status,
                created_at,
                updated_at
            )
            SELECT
                :campaignId,
                u.id,
                u.tg_id,
                'PENDING',
                NOW(),
                NOW()
            FROM users u
            WHERE u.tg_id IS NOT NULL
            ON CONFLICT (campaign_id, tg_id) DO NOTHING
            """, nativeQuery = true)
    int insertRecipientsForCampaign(@Param("campaignId") Long campaignId);
}
