package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.common.dto.StaticContentRequestDto;
import com.sdl.dxa.tridion.content.StaticContentResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Config service encapsulates logic to get settings for specific features of Model Service
 */
@Data
@Service
@Slf4j
@ConfigurationProperties(prefix = "dxa")
public class ConfigService {

    private final Errors errors = new Errors();

    private final Defaults defaults;

    @Autowired
    public ConfigService(Defaults defaults) {
        this.defaults = defaults;
    }

    @Data
    public static class Errors {

        private boolean missingKeywordSuppress;

        private boolean missingEntitySuppress;
    }

    @Data
    @Component
    public static class Defaults {

        private final StaticContentResolver staticContentResolver;

        private final ObjectMapper objectMapper;

        private String configBootstrapPath;

        private String configDcpUriField;

        @Autowired
        public Defaults(StaticContentResolver staticContentResolver, ObjectMapper objectMapper) {
            this.staticContentResolver = staticContentResolver;
            this.objectMapper = objectMapper;
        }

        /**
         * Returns the DCP template ID from CM settings file. Caches for duture use.
         *
         * @param publicationId publication id to load settings
         * @return DCP template ID
         */
        @Cacheable(value = "config", key = "{#root.methodName, #publicationId}", unless = "#result <= 0")
        public int getDynamicTemplateId(int publicationId) {
            try {
                InputStream allJson = staticContentResolver.getStaticContent(
                        StaticContentRequestDto.builder(configBootstrapPath, String.valueOf(publicationId)).build()).getContent();

                JsonNode jsonNode = objectMapper.readTree(allJson).get(configDcpUriField);

                if (jsonNode == null) {
                    log.warn("Field {} not found in {} for publication {}", configDcpUriField, configBootstrapPath, publicationId);
                    return -1;
                }

                return TcmUtils.getItemId(jsonNode.asText("tcm:0-0-0"));
            } catch (ContentProviderException | IOException e) {
                log.warn("Exception happened while loading {}, cannot get dynamicTemplateId", e);
                return -1;
            }
        }
    }
}
