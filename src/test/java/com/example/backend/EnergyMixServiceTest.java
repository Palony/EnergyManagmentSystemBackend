package com.example.backend;

import com.example.backend.client.EnergyMixClient;
import com.example.backend.dto.*;
import com.example.backend.service.EnergyMixService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EnergyMixServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EnergyMixService energyMixService;

    private EnergyMixClient energyMixClient;

    private GenerationMixDto mix(String fuel, float perc) {
        GenerationMixDto dto = new GenerationMixDto();
        dto.setFuel(fuel);
        dto.setPerc(perc);
        return dto;
    }

    private IntervalDto interval(OffsetDateTime from, OffsetDateTime to, List<GenerationMixDto> mix) {
        IntervalDto dto = new IntervalDto();
        dto.setFrom(from);
        dto.setTo(to);
        dto.setGenerationmix(mix);
        return dto;
    }

    @Nested
    class GetGeneration {
        @Test
        void returnsDataWhenApiRespondsSuccessfully() {
            OffsetDateTime from = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            OffsetDateTime to = OffsetDateTime.parse("2024-01-01T00:30:00Z");
            List<GenerationMixDto> mixes = List.of(mix("wind", 30f), mix("gas", 70f));
            IntervalDto intervalDto = interval(from, to, mixes);
            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(intervalDto));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<IntervalDto> result = energyMixClient.getGeneration(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGenerationmix()).containsExactlyElementsOf(mixes);
        }

        @Test
        void returnsEmptyListWhenApiThrowsException() {
            OffsetDateTime from = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            OffsetDateTime to = from.plusDays(1);
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenThrow(new RestClientException("API error"));
            List<IntervalDto> result = energyMixClient.getGeneration(from, to);
            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyListWhenResponseIsNull() {
            OffsetDateTime from = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            OffsetDateTime to = from.plusDays(1);
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(null);
            assertThat(energyMixClient.getGeneration(from, to)).isEmpty();
        }
    }

    @Nested
    class GetGenerationForNDays {
        @Test
        void returnsDataFromApi() {
            OffsetDateTime from = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            IntervalDto interval = interval(from, from.plusMinutes(30), List.of(mix("wind", 50f)));
            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(interval));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<IntervalDto> result = energyMixService.getGenerationForNDays(2);
            assertThat(result).hasSize(1);
            verify(restTemplate).getForObject(anyString(), eq(EnergyMixDto.class));
        }
    }

    @Nested
    class GetThreeDaysAverages {
        @Test
        void calculatesDailyAverageAndCleanEnergy() {
            ZoneId ukZone = ZoneId.of("Europe/London");
            OffsetDateTime day1 = ZonedDateTime.now(ukZone).toLocalDate().atStartOfDay(ukZone).toOffsetDateTime();
            OffsetDateTime day2 = day1.plusDays(1);

            IntervalDto i1 = interval(day1, day1.plusMinutes(30), List.of(mix("wind", 20), mix("gas", 80)));
            IntervalDto i2 = interval(day1.plusMinutes(30), day1.plusHours(1), List.of(mix("wind", 40), mix("gas", 60)));
            IntervalDto i3 = interval(day2, day2.plusMinutes(30), List.of(mix("wind", 100), mix("gas", 0)));

            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(i1, i2, i3));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<DailyMixResponse> result = energyMixService.getThreeDaysAverages();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDate()).isEqualTo(day1.toLocalDate().toString());
            assertThat(result.get(0).getAverageGeneration().get("wind")).isEqualTo(30.0);
            assertThat(result.get(0).getCleanEnergy()).isEqualTo(30.0);
            assertThat(result.get(1).getCleanEnergy()).isEqualTo(100.0);
        }

        @Test
        void ignoresInvalidIntervals() {
            ZoneId ukZone = ZoneId.of("Europe/London");
            OffsetDateTime day1 = ZonedDateTime.now(ukZone).toLocalDate().atStartOfDay(ukZone).toOffsetDateTime();

            IntervalDto valid = interval(day1, day1.plusMinutes(30), List.of(mix("solar", 50)));
            IntervalDto invalid = interval(null, null, null);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(valid, invalid));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<DailyMixResponse> result = energyMixService.getThreeDaysAverages();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAverageGeneration()).containsEntry("solar", 50.0);
        }
    }

    @Nested
    class CalculateOptimalWindow {
        @Test
        void throwsWhenHoursInvalid() {
            assertThrows(IllegalArgumentException.class, () -> energyMixService.calculateOptimalWindow(0));
            assertThrows(IllegalArgumentException.class, () -> energyMixService.calculateOptimalWindow(7));
        }

        @Test
        void findsBestWindow() {
            float[] values = {10, 20, 90, 95, 30, 40, 50, 60};
            EnergyMixDto response = new EnergyMixDto();
            response.setData(buildSequentialIntervals(values));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            OptimalWindowDto result = energyMixService.calculateOptimalWindow(1);
            assertThat(result.getCleanEnergy()).isEqualTo(92.5);
            assertThat(result.getFrom()).isEqualTo(response.getData().get(2).getFrom());
            assertThat(result.getTo()).isEqualTo(response.getData().get(3).getTo());
        }

        @Test
        void evaluatesLastPossibleWindow() {
            float[] values = {10, 20, 30, 40, 50, 60, 90, 95};
            EnergyMixDto response = new EnergyMixDto();
            response.setData(buildSequentialIntervals(values));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            OptimalWindowDto result = energyMixService.calculateOptimalWindow(1);
            assertThat(result.getCleanEnergy()).isEqualTo(92.5);
            assertThat(result.getFrom()).isEqualTo(response.getData().get(6).getFrom());
            assertThat(result.getTo()).isEqualTo(response.getData().get(7).getTo());
        }

        private List<IntervalDto> buildSequentialIntervals(float[] values) {
            List<IntervalDto> result = new ArrayList<>();
            OffsetDateTime start = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            for (int i = 0; i < values.length; i++) {
                OffsetDateTime from = start.plusMinutes(i * 30L);
                result.add(interval(from, from.plusMinutes(30), List.of(mix("wind", values[i]))));
            }
            return result;
        }
    }
}