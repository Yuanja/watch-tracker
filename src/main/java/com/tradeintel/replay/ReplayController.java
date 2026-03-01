package com.tradeintel.replay;

import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.processing.MessageProcessingService;
import com.tradeintel.replay.dto.GroupDTO;
import com.tradeintel.replay.dto.MessageSearchRequest;
import com.tradeintel.replay.dto.ReplayMessageDTO;
import com.tradeintel.replay.dto.ReplayMessageDTO.ExtractedListingRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class ReplayController {

    private static final Logger log = LogManager.getLogger(ReplayController.class);

    private final RawMessageRepository rawMessageRepository;
    private final WhatsappGroupRepository groupRepository;
    private final MessageSearchService messageSearchService;
    private final ListingRepository listingRepository;
    private final MessageProcessingService messageProcessingService;

    public ReplayController(RawMessageRepository rawMessageRepository,
                           WhatsappGroupRepository groupRepository,
                           MessageSearchService messageSearchService,
                           ListingRepository listingRepository,
                           MessageProcessingService messageProcessingService) {
        this.rawMessageRepository = rawMessageRepository;
        this.groupRepository = groupRepository;
        this.messageSearchService = messageSearchService;
        this.listingRepository = listingRepository;
        this.messageProcessingService = messageProcessingService;
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDTO>> listGroups() {
        List<WhatsappGroup> groups = groupRepository.findByIsActiveTrueOrderByGroupNameAsc();
        List<GroupDTO> dtos = groups.stream()
                .map(GroupDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/groups/{groupId}/messages")
    public ResponseEntity<Page<ReplayMessageDTO>> getGroupMessages(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        WhatsappGroup group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestampWa").descending());
        Page<RawMessage> messages = rawMessageRepository.findByGroupIdOrderByTimestampWaDesc(groupId, pageable);

        String groupName = group.getGroupName();
        Page<ReplayMessageDTO> dtos = messages.map(msg -> ReplayMessageDTO.fromEntity(msg, groupName));
        enrichWithListings(dtos.getContent());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    public ResponseEntity<Page<ReplayMessageDTO>> getMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestampWa").descending());
        Page<RawMessage> messages = rawMessageRepository.findAll(pageable);

        // Build group name lookup
        Map<UUID, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(WhatsappGroup::getId, WhatsappGroup::getGroupName));

        Page<ReplayMessageDTO> dtos = messages.map(msg ->
                ReplayMessageDTO.fromEntity(msg, msg.getGroup() != null
                        ? groupNames.getOrDefault(msg.getGroup().getId(), "Unknown")
                        : "Unknown"));
        enrichWithListings(dtos.getContent());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ReplayMessageDTO>> searchMessages(
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String semantic,
            @RequestParam(required = false) String sender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // When no groupId is specified, perform a cross-group search
        if (groupId == null) {
            Page<ReplayMessageDTO> dtos = messageSearchService.search(q, semantic, page, size);
            enrichWithListings(dtos.getContent());
            return ResponseEntity.ok(dtos);
        }

        if (!groupRepository.existsById(groupId)) {
            return ResponseEntity.notFound().build();
        }

        Page<ReplayMessageDTO> dtos = messageSearchService.searchByFilters(
                groupId, q, sender, null, null, page, size);
        enrichWithListings(dtos.getContent());
        return ResponseEntity.ok(dtos);
    }

    @AdminOnly
    @PostMapping("/{messageId}/extract")
    public ResponseEntity<ExtractedListingRef> extractSingleMessage(@PathVariable UUID messageId) {
        if (!rawMessageRepository.existsById(messageId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Listing> listings = messageProcessingService.processMessageSync(messageId);
            if (listings.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(ExtractedListingRef.fromListing(listings.get(0)));
        } catch (Exception e) {
            log.error("Extraction failed for message {}", messageId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void enrichWithListings(List<ReplayMessageDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<UUID> messageIds = dtos.stream()
                .map(ReplayMessageDTO::getId)
                .collect(Collectors.toList());
        try {
            List<Listing> listings = listingRepository.findByRawMessageIdInAndDeletedAtIsNull(messageIds);
            Map<UUID, Listing> listingByMsgId = listings.stream()
                    .collect(Collectors.toMap(
                            l -> l.getRawMessage().getId(),
                            l -> l,
                            (a, b) -> a // if multiple listings, take the first
                    ));
            for (ReplayMessageDTO dto : dtos) {
                Listing listing = listingByMsgId.get(dto.getId());
                if (listing != null) {
                    dto.setExtractedListing(ExtractedListingRef.fromListing(listing));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to enrich messages with listings (non-fatal): {}", e.getMessage());
        }
    }
}
