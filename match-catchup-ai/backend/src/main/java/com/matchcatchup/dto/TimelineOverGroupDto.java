package com.matchcatchup.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineOverGroupDto {

    private int innings;
    private int overNumber;
    private int totalRunsInOver;
    private int wicketsInOver;
    private String bowler;
    private List<TimelineDeliveryDto> deliveries;
}
