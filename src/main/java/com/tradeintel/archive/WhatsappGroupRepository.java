package com.tradeintel.archive;

import com.tradeintel.common.entity.WhatsappGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappGroupRepository extends JpaRepository<WhatsappGroup, UUID> {

    Optional<WhatsappGroup> findByWhapiGroupId(String whapiGroupId);

    List<WhatsappGroup> findByIsActiveTrueOrderByGroupNameAsc();
}
