package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.DxaModelServiceApplication;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.tridion.broker.StorageException;
import org.apache.commons.io.IOUtils;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.FactoryException;
import org.dd4t.databind.DataBindFactory;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConverterServiceTest.SpringConfigurationContext.class)
public class ConverterServiceTest {

    @Autowired
    private ObjectMapper r2ObjectMapper;

    @Mock
    private ConverterService converter;

    private PageModelData r2PageDataModel;

    private Page dd4tPageDataModel;

    private PageRequestDto pageRequestDto;

    private String dd4tSource;

    private String r2Source;

    @Before
    public void init() throws ContentProviderException, IOException, FactoryException, StorageException {

        pageRequestDto = PageRequestDto.builder()
                .publicationId(1)
                .uriType("tcm")
                .build();

        dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");

        dd4tPageDataModel = DataBindFactory.buildPage(dd4tSource, PageImpl.class);
        r2PageDataModel = r2ObjectMapper.readValue(r2Source, PageModelData.class);

        // TODO: Remove mocking when converter's implementation is ready
        when(converter.convertToDd4t(eq(r2PageDataModel), eq(pageRequestDto)))
                .thenReturn(dd4tPageDataModel);
        when(converter.convertToR2(eq(dd4tPageDataModel), eq(pageRequestDto)))
                .thenReturn(r2PageDataModel);
    }

    @Test
    public void shouldDetectCorrectModel_OfJsonContent() {
        //when
        PageRequestDto.DataModelType dd4t = ConverterService.getModelType(dd4tSource);
        PageRequestDto.DataModelType r2 = ConverterService.getModelType(r2Source);

        //then
        assertEquals(PageRequestDto.DataModelType.DD4T, dd4t);
        assertEquals(PageRequestDto.DataModelType.R2, r2);
    }

    @Test
    public void shouldConvertLegacyModelToR2() {
        //given 
        PageModelData expected = r2PageDataModel;

        //when
        PageModelData actual = converter.convertToR2(dd4tPageDataModel, pageRequestDto);

        //then
        assertEquals(actual, expected);
    }

    @Test
    public void shouldConvertR2ModelToLegacy() throws ContentProviderException {
        //given
        Page expected = dd4tPageDataModel;

        //when
        Page actual = converter.convertToDd4t(r2PageDataModel, pageRequestDto);

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
            return new DxaModelServiceApplication().dd4tDataBinder();
        }

        @Bean
        public DataBindFactory dd4tPageFactory() {
            return new DxaModelServiceApplication().dd4tPageFactory();
        }
    }
}