package com.sdl.dxa.caching;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ModelServiceLocalizationIdProvider implements LocalizationIdProvider {

    private String currentId;

    @Override
    public String getId() {
        return currentId != null ? currentId :
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURI();
    }

    public void setCurrentId(String currentId) {
        this.currentId = currentId;
    }

    public void setCurrentId(Integer currentId) {
        setCurrentId(String.valueOf(currentId));
    }
}
