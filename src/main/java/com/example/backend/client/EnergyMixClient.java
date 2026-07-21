package com.example.backend.client;

import com.example.backend.dto.IntervalDto;

import java.time.OffsetDateTime;
import java.util.List;

public interface EnergyMixClient {

    List<IntervalDto> getGeneration(OffsetDateTime from, OffsetDateTime to);

}
