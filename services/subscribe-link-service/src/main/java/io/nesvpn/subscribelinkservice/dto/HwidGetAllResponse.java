package io.nesvpn.subscribelinkservice.dto;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class HwidGetAllResponse {
    private Response response;

    @Getter
    @Data
    public static class Response {

        private int total;
        private List<HwidDevice> devices;
    }
}

