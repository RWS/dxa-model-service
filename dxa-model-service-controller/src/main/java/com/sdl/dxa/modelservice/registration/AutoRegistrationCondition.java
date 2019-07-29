package com.sdl.dxa.modelservice.registration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check whether all the dependencies and flags are present/enabled in order to proceed with auto-registration.
 */
@Slf4j
public class AutoRegistrationCondition implements Condition {

    final String VALUE_NO = "no";
    final String VALUE_FALSE = "false";
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String register = context.getEnvironment().getProperty("register");
        boolean matches = context.getResourceLoader().getResource(ModelServiceRegisterer.CONFIG_FILE_NAME).exists()
                && context.getEnvironment().containsProperty("register") && !register.equals(VALUE_FALSE) && !register.equals(VALUE_NO);
        log.debug("Auto registration of MS: {}", matches);
        return matches;
    }
}
