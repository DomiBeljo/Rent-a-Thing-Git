package org.example.rentathingproba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class RentAThingProbaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentAThingProbaApplication.class, args);
    }
}