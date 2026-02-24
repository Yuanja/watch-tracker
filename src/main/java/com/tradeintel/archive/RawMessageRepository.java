package com.tradeintel.archive;

import com.tradeintel.common.entity.RawMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawMessageRepository extends JpaRepository<RawMessage, UUID> {

    Optional<RawMessage> findByWhapiMsgId(String whapiMsgId);

    boolean existsByWhapiMsgId(String whapiMsgId);

    @Query("SELECT m FROM RawMessage m WHERE m.group.id = :groupId ORDER BY m.timestampWa DESC")
    Page<RawMessage> findByGroupIdOrderByTimestampWaDesc(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("SELECT m FROM RawMessage m WHERE m.group.id = :groupId " +
           "AND (:senderName IS NULL OR LOWER(m.senderName) LIKE LOWER(CONCAT('%', :senderName, '%'))) " +
           "AND (:dateFrom IS NULL OR m.timestampWa >= :dateFrom) " +
           "AND (:dateTo IS NULL OR m.timestampWa <= :dateTo) " +
           "ORDER BY m.timestampWa DESC")
    Page<RawMessage> findByGroupIdWithFilters(
            @Param("groupId") UUID groupId,
            @Param("senderName") String senderName,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            Pageable pageable);

    @Query("SELECT m FROM RawMessage m WHERE m.processed = false ORDER BY m.receivedAt ASC")
    Page<RawMessage> findUnprocessed(Pageable pageable);
}
