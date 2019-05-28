package com.sdl.springconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.tridion.linking.BatchLinkResolverImpl;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.mockito.Mockito.mock;

@Configuration
@PropertySource("classpath:dxa.properties")
public class BatchLinkResolverSpringConfig {

    @Bean
    public BatchLinkResolver batchLinkResolver() {
        return new BatchLinkResolverImpl(true, true, true, this.mockedLinkRetriever());
    }

    @Bean
    public BatchLinkRetriever mockedLinkRetriever() {
        return mock(BatchLinkRetrieverImpl.class);
    }

    @Bean
    public BatchLinkResolver mockedBatchLinkResolver() {
        return new BatchLinkResolverImpl(true, true, true, this.mockedLinkRetriever());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new DataModelSpringConfiguration().dxaR2ObjectMapper();
    }
}
