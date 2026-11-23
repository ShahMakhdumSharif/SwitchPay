package com.switchpay;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class SwitchPayApplication {

    private final Environment environment;

    public SwitchPayApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(SwitchPayApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
        System.out.println("\n============================================================");
        System.out.println("  SwitchPay is running!");
        System.out.println("  Website / UI (Spring Boot) : http://localhost:" + port + "/");
        System.out.println("  Website / UI (Vite Dev)    : http://localhost:5173/");
        System.out.println("  Health Check               : http://localhost:" + port + "/actuator/health");
        System.out.println("============================================================\n");
    }
}
