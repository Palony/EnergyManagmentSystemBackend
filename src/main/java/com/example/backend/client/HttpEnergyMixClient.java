package com.example.backend.client;

import com.example.backend.dto.EnergyMixDto;
import com.example.backend.dto.IntervalDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class HttpEnergyMixClient implements EnergyMixClient {

    private final RestTemplate restTemplate;

    public List<IntervalDto> getGeneration(OffsetDateTime from, OffsetDateTime to) {
        ZoneId ukZone = ZoneId.of("Europe/London");

        String fromStr = from.atZoneSameInstant(ukZone)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String toStr = to.atZoneSameInstant(ukZone)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String url = UriComponentsBuilder.fromUriString("https://api.carbonintensity.org.uk/generation")
                .pathSegment(fromStr, toStr)
                .toUriString();

        try {
            EnergyMixDto energyMixDto = restTemplate.getForObject(url, EnergyMixDto.class);

            if (energyMixDto != null && energyMixDto.getData() != null) {
                return energyMixDto.getData();
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas pobierania danych z API: " + e.getMessage());
        }

        return Collections.emptyList();
    }
}
