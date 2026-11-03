package com.matchcatchup.controller;

import com.matchcatchup.dto.ChatRequestDto;
import com.matchcatchup.dto.ChatResponseDto;
import com.matchcatchup.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final RagChatService chatService;

    @PostMapping("/api/catchup/{sessionId}/chat")
    public ChatResponseDto chat(
            @PathVariable String sessionId,
            @RequestBody ChatRequestDto request, Principal principal) {
        return chatService.processChat(sessionId, principal.getName(), request);
    }

    @PostMapping("/api/matches/{matchId}/chat")
    public ChatResponseDto matchChat(
            @PathVariable String matchId,
            @RequestBody ChatRequestDto request, Principal principal) {
        return chatService.processMatchChat(matchId, principal.getName(), request);
    }
}
