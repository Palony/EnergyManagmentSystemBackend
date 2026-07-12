package com.example.backend;

import com.example.backend.dto.DailyMixResponse;
import com.example.backend.dto.EnergyMixDto;
import com.example.backend.dto.GenerationMixDto;
import com.example.backend.dto.IntervalDto;
import com.example.backend.dto.OptimalWindowDto;
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

            List<IntervalDto> result = energyMixService.getGeneration(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGenerationmix()).containsExactlyElementsOf(mixes);
        }

        @Test
        void returnsEmptyListWhenApiThrowsException() {
            OffsetDateTime from = OffsetDateTime.now();
            OffsetDateTime to = from.plusDays(1);

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class)))
                    .thenThrow(new RestClientException("API unavailable"));

            List<IntervalDto> result = energyMixService.getGeneration(from, to);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyListWhenResponseIsNull() {
            OffsetDateTime from = OffsetDateTime.now();
            OffsetDateTime to = from.plusDays(1);

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(null);

            List<IntervalDto> result = energyMixService.getGeneration(from, to);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyListWhenResponseDataIsNull() {
            OffsetDateTime from = OffsetDateTime.now();
            OffsetDateTime to = from.plusDays(1);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(null);

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<IntervalDto> result = energyMixService.getGeneration(from, to);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetGenerationForNDays {

        @Test
        void delegatesToGetGenerationAndReturnsData() {
            List<GenerationMixDto> mixes = List.of(mix("nuclear", 50f));
            IntervalDto intervalDto = interval(OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30), mixes);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(intervalDto));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<IntervalDto> result = energyMixService.getGenerationForNDays(2);

            assertThat(result).hasSize(1);
            verify(restTemplate).getForObject(anyString(), eq(EnergyMixDto.class));
        }
    }

    @Nested
    class GetThreeDaysAverages {

        @Test
        void groupsIntervalsByDayAndComputesAveragesAndCleanEnergy() {
            OffsetDateTime day1a = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            OffsetDateTime day1b = OffsetDateTime.parse("2024-01-01T00:30:00Z");
            OffsetDateTime day2a = OffsetDateTime.parse("2024-01-02T00:00:00Z");

            IntervalDto i1 = interval(day1a, day1a.plusMinutes(30),
                    List.of(mix("wind", 20f), mix("gas", 80f)));
            IntervalDto i2 = interval(day1b, day1b.plusMinutes(30),
                    List.of(mix("wind", 40f), mix("gas", 60f)));
            IntervalDto i3 = interval(day2a, day2a.plusMinutes(30),
                    List.of(mix("wind", 100f), mix("gas", 0f)));

            EnergyMixDto response = new EnergyMixDto();
            response.setData(List.of(i1, i2, i3));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<DailyMixResponse> result = energyMixService.getThreeDaysAverages();

            assertThat(result).hasSize(2);

            DailyMixResponse dayOne = result.get(0);
            DailyMixResponse dayTwo = result.get(1);

            assertThat(dayOne.getDate()).isEqualTo("2024-01-01");
            assertThat(dayTwo.getDate()).isEqualTo("2024-01-02");

            assertThat(dayOne.getAverageGeneration().get("wind")).isEqualTo(30.0);
            assertThat(dayOne.getAverageGeneration().get("gas")).isEqualTo(70.0);

            assertThat(dayOne.getCleanEnergy()).isEqualTo(30.0);

            assertThat(dayTwo.getAverageGeneration().get("wind")).isEqualTo(100.0);
            assertThat(dayTwo.getCleanEnergy()).isEqualTo(100.0);
        }

        @Test
        void ignoresIntervalsWithNullFromOrNullGenerationMix() {
            OffsetDateTime validFrom = OffsetDateTime.parse("2024-01-01T00:00:00Z");

            IntervalDto valid = interval(validFrom, validFrom.plusMinutes(30), List.of(mix("solar", 50f)));
            IntervalDto nullFrom = interval(null, validFrom.plusMinutes(30), List.of(mix("solar", 50f)));
            IntervalDto nullMix = interval(validFrom, validFrom.plusMinutes(30), null);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(new ArrayList<>(List.of(valid, nullFrom, nullMix)));

            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            List<DailyMixResponse> result = energyMixService.getThreeDaysAverages();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAverageGeneration()).containsEntry("solar", 50.0);
        }

        @Test
        void returnsEmptyListWhenNoDataAvailable() {
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(null);

            List<DailyMixResponse> result = energyMixService.getThreeDaysAverages();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CalculateOptimalWindow {

        @Test
        void throwsWhenHoursBelowMinimum() {
            assertThrows(IllegalArgumentException.class, () -> energyMixService.calculateOptimalWindow(0));
        }

        @Test
        void throwsWhenHoursAboveMaximum() {
            assertThrows(IllegalArgumentException.class, () -> energyMixService.calculateOptimalWindow(7));
        }

        @Test
        void findsWindowWithHighestCleanEnergyAverage() {
            float[] windPercentages = {10f, 20f, 90f, 95f, 30f, 40f, 50f, 60f};
            List<IntervalDto> intervals = buildSequentialIntervals(windPercentages);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(intervals);
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            OptimalWindowDto result = energyMixService.calculateOptimalWindow(1); // windowSize = 2

            assertThat(result.getCleanEnergy()).isEqualTo(92.5);
            assertThat(result.getFrom()).isEqualTo(intervals.get(2).getFrom());
            assertThat(result.getTo()).isEqualTo(intervals.get(3).getTo());
        }

        @Test
        void currentImplementationNeverEvaluatesTheFinalWindow() {
            float[] windPercentages = {10f, 20f, 30f, 40f, 50f, 60f, 90f, 95f};
            List<IntervalDto> intervals = buildSequentialIntervals(windPercentages);

            EnergyMixDto response = new EnergyMixDto();
            response.setData(intervals);
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(response);

            OptimalWindowDto result = energyMixService.calculateOptimalWindow(1); // windowSize = 2

            assertThat(result.getCleanEnergy()).isNotEqualTo(92.5);
            assertThat(result.getCleanEnergy()).isEqualTo(75.0);
            assertThat(result.getFrom()).isEqualTo(intervals.get(5).getFrom());
            assertThat(result.getTo()).isEqualTo(intervals.get(6).getTo());
        }

        @Test
        void throwsWhenAvailableDataIsSmallerThanWindowSize() {
            when(restTemplate.getForObject(anyString(), eq(EnergyMixDto.class))).thenReturn(null);

            assertThrows(IndexOutOfBoundsException.class, () -> energyMixService.calculateOptimalWindow(6));
        }

        private List<IntervalDto> buildSequentialIntervals(float[] windPercentages) {
            List<IntervalDto> intervals = new ArrayList<>();
            OffsetDateTime start = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            for (int i = 0; i < windPercentages.length; i++) {
                OffsetDateTime from = start.plusMinutes(30L * i);
                OffsetDateTime to = from.plusMinutes(30);
                intervals.add(interval(from, to, List.of(mix("wind", windPercentages[i]))));
            }
            return intervals;
        }
    }
}