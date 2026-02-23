package io.nesvpn.telegrambot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Formatter {
        public static String formatExpiryTime(long expiresAt) {
            Instant instant = Instant.ofEpochMilli(expiresAt);
            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Europe/Moscow"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            return zonedDateTime.format(formatter);
        }

}
