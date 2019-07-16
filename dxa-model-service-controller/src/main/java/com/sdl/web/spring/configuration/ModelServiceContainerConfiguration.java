package com.sdl.web.spring.configuration;

import com.sdl.dxa.DxaModelServiceApplication;
import com.sdl.dxa.tridion.annotations.impl.ValueAnnotationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DxaModelServiceApplication.class)
public class ModelServiceContainerConfiguration implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(ModelServiceContainerConfiguration.class);

    @Value("${dxa.defaults.rich-text-xmlns-remove:not_defined}")
    private String richTextXmlnsRemove;

    @Value("${dxa.defaults.rich-text-resolve:not_defined}")
    private String richTextResolve;

    @Value("${dxa.web.link-resolver.remove-extension:not_defined}")
    private String linkResolverRemoveExtension;

    @Value("${dxa.web.link-resolver.strip-index-path:not_defined}")
    private String linkResolverStripIndexPath;

    @Value("${dxa.web.link-resolver.keep-trailing-slash:not_defined}")
    private String linkResolverKeepTrailingSlash;

    @Value("${dxa.errors.missing-keyword-suppress:not_defined}")
    private String missingKeywordSuppress;

    @Value("${dxa.errors.missing-entity-suppress:not_defined}")
    private String missingEntitySuppress;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info(new ValueAnnotationLogger().fetchAllValues(this));
    }
}