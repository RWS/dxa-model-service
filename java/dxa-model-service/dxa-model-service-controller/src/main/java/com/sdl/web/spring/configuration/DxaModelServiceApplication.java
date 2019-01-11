package com.sdl.web.spring.configuration;

import com.sdl.dxa.Dd4tSpringConfiguration;
import com.sdl.dxa.IdProviderConfiguration;
import com.sdl.dxa.caching.TridionCacheConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication(
        scanBasePackages = {
                "com.sdl.dxa.modelservice.controller",
                "com.sdl.dxa.tridion.navigation",
                "com.sdl.dxa.spring.configuration","com.sdl.dxa.caching"},
        exclude = {
                HibernateJpaAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class})
@PropertySource("classpath:dxa.properties")
@Import({IdProviderConfiguration.class, TridionCacheConfiguration.class, Dd4tSpringConfiguration.class})
public class DxaModelServiceApplication {

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }
}
