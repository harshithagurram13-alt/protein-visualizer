package com.proteinviz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProteinVizApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProteinVizApplication.class, args);
    }
}
