package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.tridion.broker.StorageException;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.BaseField;
import org.dd4t.contentmodel.impl.ComponentImpl;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.FactoryException;
import org.dd4t.databind.DataBindFactory;
import org.dd4t.databind.builder.json.JsonDataBinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConverterServiceTest.SpringConfigurationContext.class)
public class ConverterServiceTest {

    PageModelData r2PageDataModel = null;

    PageImpl dd4tPageDataModel = null;

    @Autowired
    private DataBindFactory dd4tPageFactory;

    @Autowired
    private ObjectMapper r2Mapper;

    @Mock
    private ConverterService converter;

    private PageRequestDto dto;

    @Before
    public void init() throws ContentProviderException, IOException, FactoryException, StorageException {

        dto = PageRequestDto.builder()
                .publicationId(1)
                .uriType("tcm")
                .build();

        //when
        dd4tPageDataModel = DataBindFactory.buildPage(new String(Files.readAllBytes(new ClassPathResource("models/dd4t.json").getFile().toPath())), PageImpl.class);
        r2PageDataModel = r2Mapper.readValue(new ClassPathResource("models/r2.json").getFile(), PageModelData.class);

        // TODO: Remove mocking when converter's implementation is ready
        when(converter.convertToDd4t(eq(r2PageDataModel), eq(dto)))
                .thenReturn(dd4tPageDataModel);
        when(converter.convertToR2(eq(dd4tPageDataModel), eq(dto)))
                .thenReturn(r2PageDataModel);
    }

    @Test
    public void shouldConvertLegacyModelToR2() {
        //given 
        PageModelData expected = r2PageDataModel;

        //when
        PageModelData actual = converter.convertToR2(dd4tPageDataModel, dto);

        //then
        assertEquals(actual, expected);
    }

    @Test
    public void shouldConvertR2ModelToLegacy() throws StorageException {
        //given
        PageImpl expected = dd4tPageDataModel;

        //when
        Page actual = converter.convertToDd4t(r2PageDataModel, dto);

        //then
        assertEquals(actual, expected);
    }

    @Configuration
    public static class SpringConfigurationContext {

        @Bean
        public ObjectMapper r2Mapper() {
            return new DataModelSpringConfiguration().dxaR2ObjectMapper();
        }

        @Bean
        public DataBinder dataBinder() {
            JsonDataBinder dataBinder = JsonDataBinder.getInstance();
            dataBinder.setRenderDefaultComponentModelsOnly(true);
            dataBinder.setRenderDefaultComponentsIfNoModelFound(true);
            dataBinder.setConcreteComponentImpl(ComponentImpl.class);
            // Important! If use 2.0.11 version of DD4T the you need to set concrete implementation for Component presentation an template(2 lines below)
            // In 2.2.1-DXA version everything is set under the hood
            dataBinder.setConcreteComponentPresentationImpl(ComponentPresentationImpl.class);
            dataBinder.setConcreteComponentTemplateImpl(ComponentTemplateImpl.class);

            dataBinder.setConcreteFieldImpl(BaseField.class);
            return dataBinder;
        }

        @Bean
        public DataBindFactory dd4tPageFactory() {
            DataBindFactory bindFactory = DataBindFactory.getInstance();
            bindFactory.setDataBinder(dataBinder());
            return bindFactory;
        }

    }

}