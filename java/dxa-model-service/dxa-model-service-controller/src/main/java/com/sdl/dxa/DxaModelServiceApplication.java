package com.sdl.dxa;

import com.sdl.dxa.caching.TridionCacheConfiguration;
import com.sdl.web.ambient.client.AmbientClientFilter;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.taxonomies.WebTaxonomyFactoryImpl;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:dxa.properties")
@Import({TridionCacheConfiguration.class, Dd4tSpringConfiguration.class})
public class DxaModelServiceApplication {

    @Value("${server.tomcat.protocol-header}")
    private String ipFilterProtocolHeader;

    @Value("${server.tomcat.port-header}")
    private String ipFilterPortHeader;

    @Value("${server.tomcat.remote-ip-header}")
    private String ipFilterRemoteIpHeader;

    @Value("${server.tomcat.internal-proxies}")
    private String ipFilterInternalProxies;

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }

    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }

    @Bean
    public WebTaxonomyFactory webTaxonomyFactory() {
        return new WebTaxonomyFactoryImpl();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }

    private RemoteIpValve remoteIpFilter() {
        RemoteIpValve remoteIpFilter = new RemoteIpValve();
        remoteIpFilter.setPortHeader(ipFilterPortHeader);
        remoteIpFilter.setProtocolHeader(ipFilterProtocolHeader);
        remoteIpFilter.setRemoteIpHeader(ipFilterRemoteIpHeader);
        remoteIpFilter.setInternalProxies(ipFilterInternalProxies);
        return remoteIpFilter;
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        tomcat.addContextValves(remoteIpFilter());
        return tomcat;
    }
}
