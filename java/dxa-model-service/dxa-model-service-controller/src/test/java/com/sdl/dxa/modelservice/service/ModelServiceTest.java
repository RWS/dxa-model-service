package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static com.sdl.dxa.modelservice.service.DefaultPageModelService.getModelType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DefaultEntityModelService.class)
public class ModelServiceTest {

    @Spy
    private ObjectMapper objectMapper = new DataModelSpringConfiguration().dxaR2ObjectMapper();

    @Mock
    private LinkResolver linkResolver;

    @Mock
    private ComponentPresentationFactory componentPresentationFactory;

    @Mock
    private ComponentPresentation dcp;

    @Mock
    private ConfigService configService;

    @Mock
    private ConfigService.Defaults defaults;

    @InjectMocks
    private DefaultEntityModelService modelService;

    @Before
    public void init() throws Exception {
        PowerMockito.whenNew(ComponentPresentationFactory.class).withAnyArguments().thenReturn(componentPresentationFactory);
        doReturn(dcp).when(componentPresentationFactory).getComponentPresentation(eq("tcm:42-1"), eq("tcm:42-10247-32"));

        String value = objectMapper.writeValueAsString(objectMapper.readTree(new ClassPathResource("dcp.json").getFile()));
        when(dcp.getContent()).thenReturn(value);

        when(configService.getDefaults()).thenReturn(defaults);

        when(defaults.getDynamicTemplateId(eq(42))).thenReturn(10247);
    }


    @Test
    public void shouldDetectCorrectModel_OfJsonContent() throws IOException {
        //given
        String dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        String r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");


        //when
        PageRequestDto.DataModelType dd4t = getModelType(dd4tSource);
        PageRequestDto.DataModelType r2 = getModelType(r2Source);

        //then
        assertEquals(PageRequestDto.DataModelType.DD4T, dd4t);
        assertEquals(PageRequestDto.DataModelType.R2, r2);
    }


    @Test
    public void shouldGetDefaultDynamicTemplate_ForDCP_WhenNoTemplateSet() throws ContentProviderException {
        //given 
        EntityRequestDto entityRequest = EntityRequestDto.builder().publicationId(42).componentId(1).build();

        //when
        EntityModelData entity = modelService.loadEntity(entityRequest);

        //then
        assertEquals("1468", entity.getId());
    }

}