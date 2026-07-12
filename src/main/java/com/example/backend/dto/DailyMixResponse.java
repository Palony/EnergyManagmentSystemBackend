package com.example.backend.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DailyMixResponse {
    private String date;
    private Map<String, Double> averageGeneration;
    private Double cleanEnergy;
}