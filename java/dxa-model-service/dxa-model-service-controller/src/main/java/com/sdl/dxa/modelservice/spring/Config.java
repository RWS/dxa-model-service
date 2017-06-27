package com.sdl.dxa.modelservice.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dxa")
public class Config {

    private final Errors errors = new Errors();

    private final Defaults defaults = new Defaults();

    @Data
    public static class Errors {

        private boolean missingKeywordSuppress;

        private boolean missingEntitySuppress;
    }

    @Data
    public static class Defaults {

        private int dynamicTemplateId;

        public int getDynamicTemplateId(int publicationId) {
            return 10247;
        }
    }
}
