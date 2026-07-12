package com.example.backend.dto;


import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OptimalWindowDto {
    private OffsetDateTime from;
    private OffsetDateTime to;
    private double cleanEnergy;
}
