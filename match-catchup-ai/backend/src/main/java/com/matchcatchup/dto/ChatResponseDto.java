package com.matchcatchup.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    private String answer;
    private List<ChatReferenceDto> references;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatReferenceDto {
        private int innings;
        private int over;
        private int ball;
        private String description;
    }
}
