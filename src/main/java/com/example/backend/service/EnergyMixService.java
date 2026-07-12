package com.example.backend.service;

import com.example.backend.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnergyMixService {

    private final RestTemplate restTemplate;

    public EnergyMixService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }
    public List<IntervalDto> getGeneration(OffsetDateTime from, OffsetDateTime to){
        String fromStr = DateTimeFormatter.ISO_INSTANT.format(from.toInstant());
        String toStr = DateTimeFormatter.ISO_INSTANT.format(to.toInstant());

        String url = UriComponentsBuilder.fromUriString("https://api.carbonintensity.org.uk/generation")
                .pathSegment(fromStr, toStr)
                .toUriString();

        try{
            EnergyMixDto energyMixDto = restTemplate.getForObject(url, EnergyMixDto.class);

            if(energyMixDto != null && energyMixDto.getData() != null){
                return energyMixDto.getData();
            }
        }catch(Exception e){
            System.err.println("Błąd podczas pobierania danych z API: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<IntervalDto> getGenerationForNDays(int Days){
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = OffsetDateTime.now().plusDays(Days);
        return getGeneration(from, to);
    }

    public List<DailyMixResponse> getThreeDaysAverages() {
        List<IntervalDto> rawIntervals = getGenerationForNDays(3);

        Map<LocalDate, List<IntervalDto>> groupedByDay = rawIntervals.stream()
                .filter(i -> i.getFrom() != null && i.getGenerationmix() != null)
                .collect(Collectors.groupingBy(i -> i.getFrom().toLocalDate()));

        List<DailyMixResponse> finalResult = new ArrayList<>();


        for (Map.Entry<LocalDate, List<IntervalDto>> entry : groupedByDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<IntervalDto> intervalsForThisDay = entry.getValue();

            List<GenerationMixDto> allFuelsForDay = intervalsForThisDay.stream()
                    .flatMap(i -> i.getGenerationmix().stream())
                    .collect(Collectors.toList());

            Map<String, Double> fuelAverages = allFuelsForDay.stream()
                    .collect(Collectors.groupingBy(
                            GenerationMixDto::getFuel,
                            Collectors.averagingDouble(GenerationMixDto::getPerc)
                    ));

            fuelAverages.replaceAll((fuel, average) -> Math.round(average * 100.0) / 100.0);

            Double cleanEnergy = calculateCleanEnergyForIntervals(intervalsForThisDay);


            DailyMixResponse dailyResponse = new DailyMixResponse();
            dailyResponse.setDate(date.toString());
            dailyResponse.setCleanEnergy(cleanEnergy);
            dailyResponse.setAverageGeneration(fuelAverages);

            finalResult.add(dailyResponse);
        }

        finalResult.sort(Comparator.comparing(DailyMixResponse::getDate));

        return finalResult;
    }

    private Double calculateCleanEnergyForIntervals(List<IntervalDto> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return 0.0;
        }

        List<String> cleanEnergySources = List.of("biomass", "nuclear", "hydro", "wind", "solar");
        double totalCleanPercentageInWindow = 0.0;

        for (IntervalDto interval : intervals) {
            if (interval.getGenerationmix() == null) continue;

            for (GenerationMixDto mix : interval.getGenerationmix()) {
                if (cleanEnergySources.contains(mix.getFuel().toLowerCase())) {
                    totalCleanPercentageInWindow += mix.getPerc();
                }
            }
        }

        double average = totalCleanPercentageInWindow / intervals.size();

        return Math.round(average * 100.0) / 100.0;
    }

    public OptimalWindowDto calculateOptimalWindow(int hours) {
        if (hours < 1 || hours > 6) {
            throw new IllegalArgumentException("Długość okna musi być między 1 a 6 godzin.");
        }
        List<IntervalDto> generationForTwoDays = getGenerationForNDays(2);

        int windowSize = hours * 2;

        double maxCleanEnergy = -1.0;
        OffsetDateTime bestFrom = null;
        OffsetDateTime bestTo = null;

        List<IntervalDto> intervals = new ArrayList<>();

        for(int i =0; i < windowSize ; i++){
            intervals.add(generationForTwoDays.get(i));
        }

        for(int i = windowSize; i < generationForTwoDays.size(); i++){
            double avg = calculateCleanEnergyForIntervals(intervals);

            if(avg > maxCleanEnergy){
                maxCleanEnergy = avg;
                bestFrom = intervals.getFirst().getFrom();
                bestTo = intervals.getLast().getTo();
            }

            if (i < generationForTwoDays.size()) {
                intervals.removeFirst();
                intervals.add(generationForTwoDays.get(i));
            }
        }
        OptimalWindowDto result = new OptimalWindowDto();
        result.setTo(bestTo);
        result.setFrom(bestFrom);
        result.setCleanEnergy(maxCleanEnergy);

        return result;
    }

}