package com.sdl.dxa.modelservice.registration;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AutoRegistrationConditionTest {

    private static final AutoRegistrationCondition CONDITION = new AutoRegistrationCondition();

    private AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @After
    public void noInteractionsWithType() {
        verifyZeroInteractions(metadata);
    }

    @Test
    public void autoRegistrationIsEnabled_IfConfigFileIsPresent_AndPropertySet() {
        //given
        ConditionContext conditionContext = mockContext(true, true);

        //when
        boolean matches = CONDITION.matches(conditionContext, metadata);

        //then
        assertTrue(matches);
    }

    private ConditionContext mockContext(boolean resourceExists, boolean propertyExists) {
        ConditionContext conditionContext = mock(ConditionContext.class);

        ResourceLoader loader = mock(ResourceLoader.class);
        when(conditionContext.getResourceLoader()).thenReturn(loader);
        Resource resource = mock(Resource.class);
        when(loader.getResource(eq(ModelServiceRegisterer.CONFIG_FILE_NAME))).thenReturn(resource);
        when(resource.exists()).thenReturn(resourceExists);

        Environment environment = mock(Environment.class);
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.containsProperty("register")).thenReturn(propertyExists);

        return conditionContext;
    }

    @Test
    public void autoRegistrationIsDisabled_IfConfigFileIsNotPresent_AndPropertySet() {
        //given
        ConditionContext conditionContext = mockContext(false, true);

        //when
        boolean matches = CONDITION.matches(conditionContext, metadata);

        //then
        assertFalse(matches);
    }

    @Test
    public void autoRegistrationIsDisabled_IfConfigFileIsPresent_AndPropertyNotSet() {
        //given
        ConditionContext conditionContext = mockContext(true, false);

        //when
        boolean matches = CONDITION.matches(conditionContext, metadata);

        //then
        assertFalse(matches);
    }

    @Test
    public void autoRegistrationIsDisabled_IfConfigFileIsNotPresent_AndPropertyNotSet() {
        //given
        ConditionContext conditionContext = mockContext(false, false);

        //when
        boolean matches = CONDITION.matches(conditionContext, metadata);

        //then
        assertFalse(matches);
    }
}