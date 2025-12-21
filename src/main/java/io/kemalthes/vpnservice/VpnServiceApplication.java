package io.kemalthes.vpnservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VpnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpnServiceApplication.class, args);
    }
}
