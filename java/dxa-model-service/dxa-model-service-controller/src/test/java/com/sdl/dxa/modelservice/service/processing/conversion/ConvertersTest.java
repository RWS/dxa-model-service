package com.sdl.dxa.modelservice.service.processing.conversion;

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
@ContextConfiguration(classes = ConvertersTest.SpringConfigurationContext.class)
public class ConvertersTest {

    @Autowired
    private ObjectMapper r2ObjectMapper;

    @Mock
    private ToDd4tConverter toDd4tConverter;

    @Mock
    private ToR2Converter toR2Converter;

    private PageModelData r2PageDataModel;

    private Page dd4tPageDataModel;

    private PageRequestDto pageRequestDto;

    @Before
    public void init() throws ContentProviderException, IOException, FactoryException, StorageException {

        pageRequestDto = PageRequestDto.builder()
                .publicationId(1)
                .uriType("tcm")
                .build();

        String dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        String r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");

        dd4tPageDataModel = DataBindFactory.buildPage(dd4tSource, PageImpl.class);
        r2PageDataModel = r2ObjectMapper.readValue(r2Source, PageModelData.class);

        // TODO: Remove mocking when toDd4tConverter's implementation is ready
        when(toDd4tConverter.convertToDd4t(eq(r2PageDataModel), eq(pageRequestDto)))
                .thenReturn(dd4tPageDataModel);
        when(toR2Converter.convertToR2(eq(dd4tPageDataModel), eq(pageRequestDto)))
                .thenReturn(r2PageDataModel);
    }


    @Test
    public void shouldConvertLegacyModelToR2() {
        //given 
        PageModelData expected = r2PageDataModel;

        //when
        PageModelData actual = toR2Converter.convertToR2(dd4tPageDataModel, pageRequestDto);

        //then
        assertEquals(actual, expected);
    }

    @Test
    public void shouldConvertR2ModelToLegacy() throws ContentProviderException {
        //given
        Page expected = dd4tPageDataModel;

        //when
        Page actual = toDd4tConverter.convertToDd4t(r2PageDataModel, pageRequestDto);

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