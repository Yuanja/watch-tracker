package com.tradeintel.replay;

import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.replay.dto.GroupDTO;
import com.tradeintel.replay.dto.MessageSearchRequest;
import com.tradeintel.replay.dto.ReplayMessageDTO;
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

    public ReplayController(RawMessageRepository rawMessageRepository,
                           WhatsappGroupRepository groupRepository,
                           MessageSearchService messageSearchService) {
        this.rawMessageRepository = rawMessageRepository;
        this.groupRepository = groupRepository;
        this.messageSearchService = messageSearchService;
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
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ReplayMessageDTO>> searchMessages(
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (groupId == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!groupRepository.existsById(groupId)) {
            return ResponseEntity.notFound().build();
        }

        Page<ReplayMessageDTO> dtos = messageSearchService.searchByFilters(
                groupId, sender, null, null, page, size);
        return ResponseEntity.ok(dtos);
    }
}
