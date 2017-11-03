package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.DxaModelServiceApplication;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.dxa.modelservice.service.LegacyEntityModelService;
import com.sdl.web.model.PageMetaImpl;
import com.sdl.web.model.PublicationMetaImpl;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.impl.localization.semantics.JsonSchema;
import com.tridion.broker.StorageException;
import com.tridion.storage.PageMeta;
import org.apache.commons.io.IOUtils;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.FactoryException;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.databind.DataBindFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConvertersTest.SpringConfigurationContext.class)
@ActiveProfiles("test")
public class ConvertersTest {

    @Autowired
    private ObjectMapper r2ObjectMapper;

    @Autowired
    private ToDd4tConverter toDd4tConverter;

    @Autowired
    private ToR2Converter toR2Converter;

    @Autowired
    private ContentService contentService;

    @Autowired
    private PageRequestDto pageRequestDto;

    private PageModelData r2PageDataModel;

    private Page dd4tPageDataModel;

    @Before
    public void init() throws ContentProviderException, IOException, FactoryException, StorageException {
        String dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        String r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");

        dd4tPageDataModel = DataBindFactory.buildPage(dd4tSource, PageImpl.class);
        r2PageDataModel = r2ObjectMapper.readValue(r2Source, PageModelData.class);

        doReturn(IOUtils.toString(new ClassPathResource("models/dd4t_header.json").getInputStream(), "UTF-8"))
                .when(contentService).loadPageContent(pageRequestDto.toBuilder().path("/example/system/include/header").build());

        doReturn(IOUtils.toString(new ClassPathResource("models/dd4t_footer.json").getInputStream(), "UTF-8"))
                .when(contentService).loadPageContent(pageRequestDto.toBuilder().path("/example/system/include/footer").build());


        doReturn(IOUtils.toString(new ClassPathResource("models/navigation.json").getInputStream(), "UTF-8"))
                .when(contentService).loadPageContent(pageRequestDto.toBuilder().path("/example/navigation.json").build());
    }


    @Test
    public void shouldReturnNull_IfModelToConvert_IsNull() throws ContentProviderException {
        assertNull(toDd4tConverter.convertToDd4t(null, pageRequestDto));
        assertNull(toR2Converter.convertToR2(null, pageRequestDto));
    }

    @Test
    public void shouldConvertLegacyModelToR2() throws ContentProviderException {
        //given 
        PageModelData expected = r2PageDataModel;

        //when
        PageModelData actual = toR2Converter.convertToR2(dd4tPageDataModel, pageRequestDto);

        //then
        assertEquals(removeUnwantedFields(expected), removeUnwantedFields(actual));
    }

    @Test
    @Ignore("Ignored until fully implemented, need to align JSONs too")
    public void shouldConvertR2ModelToLegacy() throws ContentProviderException {
        //given
        Page expected = dd4tPageDataModel;

        //when
        Page actual = toDd4tConverter.convertToDd4t(r2PageDataModel, pageRequestDto);

        //then
        assertEquals(expected, actual);
    }

    private PageModelData removeUnwantedFields(PageModelData pageModelData) {
        // todo if possible find better solution
        pageModelData.setXpmMetadata(null);
        pageModelData.getPageTemplate().setRevisionDate(null);
        pageModelData.getRegions().forEach(regionModelData -> {
            regionModelData.setXpmMetadata(null);
            if (regionModelData.getEntities() != null) {
                regionModelData.getEntities().forEach(entityModelData -> {
                    entityModelData.setXpmMetadata(null);
                    entityModelData.getComponentTemplate().setRevisionDate(null);
                });
            }
        });
        return pageModelData;
    }

    @Configuration
    public static class SpringConfigurationContext {

        @Bean
        public ToDd4tConverter toDd4tConverter() throws ContentProviderException, IOException, SerializationException {
            return new ToDd4tConverterImpl(
                    contentService(),
                    entityModelService(),
                    configService(),
                    r2Mapper(),
                    metadataService());
        }

        @Bean
        public ToR2Converter toR2Converter() throws ContentProviderException, IOException, SerializationException {
            return new ToR2ConverterImpl(
                    contentService(),
                    r2Mapper(),
                    metadataService(),
                    linkResolver());
        }

        @Bean
        public ContentService contentService() {
            return mock(ContentService.class);
        }

        @Bean
        public LinkResolver linkResolver() {
            return mock(LinkResolver.class);
        }

        @Bean
        public EntityModelService entityModelService() {
            return mock(EntityModelService.class);
        }

        @Bean
        public LegacyEntityModelService legacyEntityModelService() {
            return mock(LegacyEntityModelService.class);
        }

        @Bean
        public ConfigService configService() throws IOException, ContentProviderException {
            ConfigService.Defaults defaults = mock(ConfigService.Defaults.class);
            ConfigService configService = new ConfigService(defaults);
            ObjectMapper objectMapper = r2Mapper();
            //noinspection unchecked
            doReturn(((List<JsonSchema>) objectMapper.readValue(IOUtils.toString(new ClassPathResource("models/schemas.json").getInputStream(), "UTF-8"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonSchema.class)))
                    .parallelStream().collect(Collectors.toMap(schema -> String.valueOf(schema.getId()), schema -> schema)))
                    .when(defaults).getSchemasJson(eq(1065));
            return configService;
        }

        @Bean
        public MetadataService metadataService() throws ContentProviderException {
            MetadataService metadataService = mock(MetadataService.class);

            PublicationMetaImpl publicationMeta = new PublicationMetaImpl();
            publicationMeta.setId(1065);
            publicationMeta.setPublicationUrl("/example");
            doReturn(publicationMeta).when(metadataService).getPublicationMeta(eq(1065));

            PublicationMetaImpl owningPublicationMeta = new PublicationMetaImpl();
            owningPublicationMeta.setId(1068);
            owningPublicationMeta.setPublicationUrl("/owning");
            doReturn(owningPublicationMeta).when(metadataService).getPublicationMeta(eq(1068));

            doReturn(getPageMeta("/autotest-parent/test_article_page.html", "test_article_page"))
                    .when(metadataService).getPageMeta(eq(1065), eq("tcm:1065-9786-64"));
            doReturn(getPageMeta(null, "system/include/header.html")).when(metadataService).getPageMeta(eq(1065), eq("tcm:1065-1480-64"));
            doReturn(getPageMeta(null, "footer.html")).when(metadataService).getPageMeta(eq(1065), eq("tcm:1065-1489-64"));

            return metadataService;
        }

        @NotNull
        private PageMetaImpl getPageMeta(String url, String filename) {
            PageMeta pageMeta = new PageMeta();
            pageMeta.setUrl(url);
            pageMeta.setFileName(filename);
            pageMeta.setOwningPublication(1068);
            return new PageMetaImpl(pageMeta);
        }


        @Bean
        public ObjectMapper r2Mapper() {
            return new DataModelSpringConfiguration().dxaR2ObjectMapper();
        }

        @Bean
        public DataBinder dataBinder() {
            return new DxaModelServiceApplication().dd4tDataBinder();
        }

        @Bean
        public DataBindFactory dd4tPageFactory() {
            return new DxaModelServiceApplication().dd4tPageFactory();
        }

        @Bean
        public PageRequestDto pageRequestDto() {
            return PageRequestDto.builder(1065, "tcm").build();
        }
    }
}