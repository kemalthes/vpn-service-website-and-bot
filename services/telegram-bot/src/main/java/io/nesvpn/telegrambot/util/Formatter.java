package io.nesvpn.telegrambot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class Formatter {
    public static String getCrypt5Url(String tokenUrl) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            String jsonBody = mapper.writeValueAsString(Map.of("url", tokenUrl));

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://crypto.happ.su/api-v2.php"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = mapper.readTree(response.body());
            return jsonNode.get("encrypted_link").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
