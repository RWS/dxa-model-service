package com.sdl.web.test;

import com.sdl.dxa.DxaModelServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class DxaOdataModelServiceTestApplication {

    //This class is used in development to start Model service with the (test-)configuration files in src/test/resources
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
    }
}
