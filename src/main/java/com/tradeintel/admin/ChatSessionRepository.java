package com.tradeintel.admin;

import com.tradeintel.common.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChatSession} entities.
 *
 * <p>Provides queries used by the user's own chat UI (filtered by userId) and
 * by uber_admin to browse all sessions across all users.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Returns a page of chat sessions belonging to the given user, most
     * recently updated first.
     *
     * @param userId   the user's UUID
     * @param pageable pagination and sorting parameters
     * @return page of chat sessions ordered by {@code updated_at} descending
     */
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Returns a page of all chat sessions across all users, most recently
     * updated first. Available to uber_admin only.
     *
     * @param pageable pagination and sorting parameters
     * @return page of chat sessions ordered by {@code updated_at} descending
     */
    Page<ChatSession> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
