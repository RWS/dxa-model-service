package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.common.dto.StaticContentRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.models.LightSchema;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        private String mappingsSchemas;

        @Autowired
        public Defaults(StaticContentResolver staticContentResolver, ObjectMapper objectMapper) {
            this.staticContentResolver = staticContentResolver;
            this.objectMapper = objectMapper;
        }

        @Cacheable(value = "config", key = "{#root.methodName, #publicationId}", unless = "#result != null")
        public JsonNode getCoreResources(int publicationId) {
            String coreFilePath = "/system/resources/core.json";
            try {
                StaticContentRequestDto staticContentRequestDto = StaticContentRequestDto.builder("/system/resources/core.json", String.valueOf(publicationId)).build();
                InputStream allJson = staticContentResolver.getStaticContent(staticContentRequestDto).getContent();
                JsonNode jsonNode = objectMapper.readTree(allJson);

                if (jsonNode == null) {
                    log.warn("File {} not found for publication {}", coreFilePath, publicationId);
                    return null;
                }

                return jsonNode;
            } catch (ContentProviderException | IOException e) {
                log.warn("Exception happened while loading {}", coreFilePath, e);
                return null;
            }
        }


        /**
         * Returns the DCP template ID from CM settings file. Caches for future use.
         *
         * @param publicationId publication id to load settings
         * @return DCP template ID
         */
        @Cacheable(value = "config", key = "{#root.methodName, #publicationId}", unless = "#result <= 0")
        public int getDynamicTemplateId(int publicationId) {
            try {
                StaticContentRequestDto staticContentRequestDto = StaticContentRequestDto.builder(configBootstrapPath, String.valueOf(publicationId)).build();
                InputStream allJson = staticContentResolver.getStaticContent(staticContentRequestDto).getContent();

                JsonNode jsonNode = objectMapper.readTree(allJson).get(configDcpUriField);

                if (jsonNode == null) {
                    log.warn("Field {} not found in {} for publication {}", configDcpUriField, configBootstrapPath, publicationId);
                    return -1;
                }

                return TcmUtils.getItemId(jsonNode.asText("tcm:0-0-0"));
            } catch (ContentProviderException | IOException e) {
                log.warn("Exception happened while loading {}, cannot get dynamicTemplateId", configBootstrapPath, e);
                return -1;
            }
        }

        /**
         * Returns the DCP template ID from CM settings file. Caches for future use.
         *
         * @param publicationId publication id to load core configuration
         * @return DCP template ID
         */
        @Cacheable(value = "config", key = "{#root.methodName, #publicationId}", unless = "#result != null")
        public String getCulture(int publicationId) {
            String coreConfigFile = "/system/config/core.json";
            try {
                StaticContentRequestDto staticContentRequestDto = StaticContentRequestDto.builder(coreConfigFile, String.valueOf(publicationId)).build();
                InputStream allJson = staticContentResolver.getStaticContent(staticContentRequestDto).getContent();

                JsonNode jsonNode = objectMapper.readTree(allJson);

                if (jsonNode == null) {
                    log.warn("Field {} not found in {} file for publication {}", "culture", coreConfigFile, publicationId);
                    return null;
                }

                return jsonNode.get("culture").asText();
            } catch (ContentProviderException | IOException e) {
                log.warn("Exception happened while loading {}, cannot get culture", coreConfigFile, e);
                return null;
            }
        }

        /**
         * Loads {@code schemas.json} configuration for current publication.
         *
         * @param publicationId publication id to load {@code schemas.json}
         * @return map of schemas indexed by ID representing schemas for this publication
         */
        @Cacheable(value = "config", key = "{#root.methodName, #publicationId}")
        public Map<String, LightSchema> getSchemasJson(int publicationId) throws ContentProviderException {
            try {
                StaticContentRequestDto staticContentRequestDto = StaticContentRequestDto.builder(mappingsSchemas, String.valueOf(publicationId)).build();
                InputStream allJson = staticContentResolver.getStaticContent(staticContentRequestDto).getContent();
                List<LightSchema> schemas = objectMapper.readValue(allJson, objectMapper.getTypeFactory().constructCollectionType(List.class, LightSchema.class));
                return schemas.parallelStream().collect(Collectors.toMap(LightSchema::getId, schema -> schema));
            } catch (IOException e) {
                throw new ContentProviderException("Exception happened while loading schemas.json, cannot get schemas config", e);
            }
        }
    }
}
