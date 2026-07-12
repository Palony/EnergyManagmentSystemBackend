package com.example.backend.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class IntervalDto {
    private OffsetDateTime from;
    private OffsetDateTime to;
    private List<GenerationMixDto> generationmix;
}
