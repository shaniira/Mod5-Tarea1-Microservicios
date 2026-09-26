package com.andinaseguros.frameworksdrivers.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.andinaseguros")
public class MotorTarificacionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MotorTarificacionApplication.class, args);
    }
}
