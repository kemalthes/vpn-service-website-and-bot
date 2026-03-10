package io.nesvpn.telegrambot.dto;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class HwidApiResponse {
    private Response response;

    @Getter
    @Data
    public static class Response {

        private int total;
        private List<HwidDevice> devices;
    }
}
