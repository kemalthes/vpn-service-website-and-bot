package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.BroadcastRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BroadcastRecipientRepository extends JpaRepository<BroadcastRecipient, Long> {

    @Query(value = """
            SELECT br.*
            FROM broadcast_recipients br
            JOIN broadcast_campaigns bc
              ON bc.id = br.campaign_id
            WHERE br.status = 'PENDING'
              AND bc.status = 'PROCESSING'
            ORDER BY br.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<BroadcastRecipient> findPendingRecipientsForProcessingCampaigns(@Param("limit") int limit);

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

    @Modifying
    @Query(value = """
            DELETE FROM broadcast_recipients br
            USING broadcast_campaigns bc
            WHERE bc.id = br.campaign_id
              AND bc.status IN ('COMPLETED', 'FAILED')
              AND br.created_at < :createdBefore
            """, nativeQuery = true)
    int deleteFinishedCampaignRecipientsCreatedBefore(@Param("createdBefore") LocalDateTime createdBefore);
}
