package com.sdl.dxa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.caching.TridionCacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;


@SpringBootApplication(
        exclude = {
            HibernateJpaAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class})
@PropertySource("classpath:dxa.properties")
@Import({ModelServiceConfiguration.class, TridionCacheConfiguration.class, Dd4tSpringConfiguration.class})
public class DxaModelServiceApplication extends WebMvcConfigurerAdapter {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {


        converters.add(
                new MappingJackson2HttpMessageConverter(applicationContext.getBean(ObjectMapper.class)));
        super.configureMessageConverters(converters);
    }

}
