# Energy Management System — Backend

Backend aplikacji do prezentacji miksu energetycznego Wielkiej Brytanii
oraz wyznaczania optymalnego okna ładowania samochodu elektrycznego.

## Technologie
- Java 21, Spring Boot 4.1.0
- Maven

## Endpointy
- `GET /api/generation` — zwraca miks energetyczny dla dziś/jutro/pojutrze
- `GET /api/optimal-Window/{hours}` — zwraca optymalne okno ładowania (1-6h)

## Uruchomienie lokalne
\```bash
mvn spring-boot:run
\```
Aplikacja wystartuje na `http://localhost:8080`.

## Uruchomienie testów
\```bash
mvn test
\```

## Zewnętrzne API
Korzysta z [Carbon Intensity API](https://carbon-intensity.github.io/api-definitions/)