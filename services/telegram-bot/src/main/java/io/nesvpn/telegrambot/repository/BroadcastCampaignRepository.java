package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.dto.broadcast.BroadcastProgressProjection;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStatsProjection;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastCampaignRepository extends JpaRepository<BroadcastCampaign, Long> {

    Optional<BroadcastCampaign> findFirstByStatusInOrderByCreatedAtAsc(Collection<BroadcastCampaignStatus> statuses);

    Optional<BroadcastCampaign> findBySourceChatIdAndSourceMessageId(Long sourceChatId, Integer sourceMessageId);

    @Query(value = """
            SELECT
                c.id AS "campaignId",
                c.source AS "source",
                c.status AS "status",
                c.total_recipients AS "totalRecipients",
                COUNT(r.id) FILTER (WHERE r.status = 'SENT') AS "sentCount",
                COUNT(r.id) FILTER (WHERE r.status = 'FAILED') AS "failedCount",
                COUNT(r.id) FILTER (WHERE r.status = 'PENDING') AS "pendingCount"
            FROM broadcast_campaigns c
            LEFT JOIN broadcast_recipients r
              ON r.campaign_id = c.id
            WHERE c.id = :campaignId
            GROUP BY c.id
            """, nativeQuery = true)
    Optional<BroadcastProgressProjection> findProgressByCampaignId(@Param("campaignId") Long campaignId);

    @Query(value = """
            WITH ready AS (
                SELECT
                    c.id AS campaign_id,
                    c.source AS source,
                    c.total_recipients AS total_recipients,
                    COUNT(r.id) FILTER (WHERE r.status = 'SENT') AS sent_count,
                    COUNT(r.id) FILTER (WHERE r.status = 'FAILED') AS failed_count
                FROM broadcast_campaigns c
                LEFT JOIN broadcast_recipients r
                  ON r.campaign_id = c.id
                WHERE c.status = 'PROCESSING'
                GROUP BY c.id, c.source, c.total_recipients, c.created_at
                HAVING COUNT(r.id) FILTER (WHERE r.status = 'PENDING') = 0
                ORDER BY c.created_at ASC
                LIMIT :limit
            ),
            updated AS (
                UPDATE broadcast_campaigns c
                SET status = 'COMPLETED',
                    completed_at = NOW()
                FROM ready
                WHERE c.id = ready.campaign_id
                RETURNING c.id
            )
            SELECT
                ready.campaign_id AS "campaignId",
                ready.source AS "source",
                ready.total_recipients AS "totalRecipients",
                ready.sent_count AS "sentCount",
                ready.failed_count AS "failedCount"
            FROM ready
            JOIN updated
              ON updated.id = ready.campaign_id
            ORDER BY ready.campaign_id ASC
            """, nativeQuery = true)
    List<BroadcastStatsProjection> completeReadyCampaigns(@Param("limit") int limit);
}
