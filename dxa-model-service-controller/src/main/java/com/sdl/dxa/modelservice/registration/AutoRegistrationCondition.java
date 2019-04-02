package com.sdl.dxa.modelservice.registration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check whether all the dependencies and flags are present/enabled in order to proceed with auto-registration.
 */
@Slf4j
public class AutoRegistrationCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean matches = context.getResourceLoader().getResource(ModelServiceRegisterer.CONFIG_FILE_NAME).exists()
                && context.getEnvironment().containsProperty("register");
        log.debug("Auto registration of MS: {}", matches);
        return matches;
    }
}
