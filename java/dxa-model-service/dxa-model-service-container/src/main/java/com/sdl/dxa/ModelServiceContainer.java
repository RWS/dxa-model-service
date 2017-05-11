package com.sdl.dxa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(DxaModelServiceApplication.class)
public class ModelServiceContainer {

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(ModelServiceContainer.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    }
}
