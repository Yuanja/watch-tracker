package com.tradeintel.admin;

import com.tradeintel.common.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChatMessage} entities.
 *
 * <p>Provides ordered retrieval of messages within a session, used both by the
 * chat AI to reconstruct conversation context and by uber_admin to inspect
 * individual session transcripts.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Returns all messages belonging to the given session in chronological order.
     * The full message list is required to rebuild the OpenAI conversation history.
     *
     * @param sessionId the UUID of the parent chat session
     * @return list of messages ordered by {@code created_at} ascending
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
