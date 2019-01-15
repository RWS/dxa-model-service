package com.sdl.dxa;

import org.dd4t.caching.providers.NoCacheProvider;
import org.dd4t.contentmodel.impl.BaseField;
import org.dd4t.contentmodel.impl.ComponentImpl;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.processors.impl.RichTextResolver;
import org.dd4t.core.processors.impl.RichTextWithLinksResolver;
import org.dd4t.core.resolvers.LinkResolver;
import org.dd4t.core.resolvers.impl.DefaultLinkResolver;
import org.dd4t.databind.builder.json.JsonDataBinder;
import org.dd4t.providers.LinkProvider;
import org.dd4t.providers.PayloadCacheProvider;
import org.dd4t.providers.impl.BrokerLinkProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class Dd4tSpringConfiguration {

    @Bean
    public DataBinder dd4tDataBinder() {
        JsonDataBinder dataBinder = new JsonDataBinder();
        dataBinder.setRenderDefaultComponentModelsOnly(true);
        dataBinder.setRenderDefaultComponentsIfNoModelFound(true);
        dataBinder.setConcreteComponentImpl(ComponentImpl.class);
        dataBinder.setConcreteComponentPresentationImpl(ComponentPresentationImpl.class);
        dataBinder.setConcreteComponentTemplateImpl(ComponentTemplateImpl.class);
        dataBinder.setConcreteFieldImpl(BaseField.class);
        return dataBinder;
    }

    @Bean
    public LinkResolver dd4tLinkResolver() {
        DefaultLinkResolver linkResolver = new DefaultLinkResolver();
        linkResolver.setLinkProvider(dd4tLinkProvider());
        return linkResolver;
    }

    @Bean
    public LinkProvider dd4tLinkProvider() {
        return new BrokerLinkProvider();
    }

    @Bean
    public RichTextResolver dd4tRichTextWithLinksResolver() {
        RichTextWithLinksResolver dd4tRichTextResolver = new RichTextWithLinksResolver();
        dd4tRichTextResolver.setLinkResolver(dd4tLinkResolver());
        return dd4tRichTextResolver;
    }

    @Bean
    public PayloadCacheProvider payloadCacheProvider() {
        return new NoCacheProvider();
    }
}
