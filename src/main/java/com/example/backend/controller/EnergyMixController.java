package com.example.backend.controller;


import com.example.backend.dto.DailyMixResponse;
import com.example.backend.dto.IntervalDto;
import com.example.backend.dto.OptimalWindowDto;
import com.example.backend.service.EnergyMixService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnergyMixController {

    private final EnergyMixService energyMixService;

    @GetMapping("/generation")
    public ResponseEntity<List<DailyMixResponse>> getEnergyMix() {
        List<DailyMixResponse> data = energyMixService.getThreeDaysAverages();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/optimal-Window/{hours}")
    public ResponseEntity<OptimalWindowDto> getOptimalWindow(@PathVariable int hours) {
        OptimalWindowDto optimalWindowDto = energyMixService.calculateOptimalWindow(hours);
        return ResponseEntity.ok(optimalWindowDto);
    }

    @GetMapping("/generation/raw")
    public ResponseEntity<List<IntervalDto>> getGenerationRaw() {



        return ResponseEntity.ok(energyMixService.getGenerationForNDays(3));
    }
}
