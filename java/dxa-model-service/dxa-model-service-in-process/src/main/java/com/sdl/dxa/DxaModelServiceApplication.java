package com.sdl.dxa;

import com.sdl.dxa.caching.TridionCacheConfiguration;
import com.sdl.dxa.tridion.linking.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.BrokerTridionLinkResolver;
import com.sdl.dxa.tridion.linking.TridionBatchLinkResolver;
import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.web.api.dynamic.formatter.WebTaxonomyFormatter;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.dynamic.taxonomies.filters.WebTaxonomyFilter;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.TaxonomyFactory;
import com.tridion.taxonomies.TaxonomyRelationManager;
import com.tridion.taxonomies.filters.TaxonomyFilter;
import com.tridion.taxonomies.formatters.TaxonomyFormatter;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@SpringBootApplication(
        exclude = {
                HibernateJpaAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class})

@PropertySource("classpath:dxa.properties")
@Import({TridionCacheConfiguration.class, Dd4tSpringConfiguration.class})
public class DxaModelServiceApplication {

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }


    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }

    @Bean
    public WebTaxonomyFactory webTaxonomyFactory() {
        return new WebTaxonomyFactory() {

            // In Process Taxonomy Factory.
            final TaxonomyFactory taxonomyFactory = new TaxonomyFactory();

            @Override
            public String[] getTaxonomies(final String publicationURI) {
                return taxonomyFactory.getTaxonomies(publicationURI);
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI);
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFormatter taxonomyFormatter) {

                final TaxonomyFormatter formatter = getTaxonomyFormatter(taxonomyFormatter);

                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI, formatter);
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter));
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter,
                                               final WebTaxonomyFormatter taxonomyFormatter) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter),
                        getTaxonomyFormatter(taxonomyFormatter));
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter,
                                               final String keywordContextURI) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter),
                        keywordContextURI);
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter,
                                               final String contextElementURI,
                                               final WebTaxonomyFormatter taxonomyFormatter) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter), contextElementURI,
                        getTaxonomyFormatter(taxonomyFormatter));
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter,
                                               final String contextKeyword, final Keyword[] selectedKeywords) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter),
                        contextKeyword, selectedKeywords);
            }

            @Override
            public Keyword getTaxonomyKeywords(final String taxonomyURI, final WebTaxonomyFilter taxonomyFilter,
                                               final String contextElementURI, final Keyword[] selectedKeywords,
                                               final WebTaxonomyFormatter taxonomyFormatter) {
                return taxonomyFactory.getTaxonomyKeywords(taxonomyURI,
                        getTaxonomyFilter(taxonomyFilter),
                        contextElementURI,
                        selectedKeywords, getTaxonomyFormatter(taxonomyFormatter));
            }

            @Override
            public Keyword getTaxonomyKeyword(final String keywordURI) {
                return taxonomyFactory.getTaxonomyKeyword(keywordURI);
            }

            @NotNull
            private TaxonomyFilter getTaxonomyFilter(final WebTaxonomyFilter taxonomyFilter) {
                return new TaxonomyFilter() {
                    @Override
                    public String filterTaxonomyContext() {
                        return taxonomyFilter.filterTaxonomyContext();
                    }

                    @Override
                    public String getFilterName() {
                        return taxonomyFilter.getFilterName();
                    }
                };
            }

            @NotNull
            private TaxonomyFormatter getTaxonomyFormatter(final WebTaxonomyFormatter taxonomyFormatter) {
                return new TaxonomyFormatter() {
                    @Override
                    public Keyword finalizeFiltering(final List<Keyword> list) {
                        return taxonomyFormatter.finalizeFiltering(list);
                    }
                };
            }
        };
    }

    @Bean
    @Primary
    public BatchLinkResolver batchLinkResolver() {
        return new TridionBatchLinkResolver();
    }

    @Bean(name = "dxaLinkResolver")
    public TridionLinkResolver linkResolver() {
        return new BrokerTridionLinkResolver();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }
}
