package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.meta.ComponentMeta;
import com.tridion.meta.ComponentMetaFactory;
import com.tridion.meta.PageMeta;
import com.tridion.meta.PageMetaFactory;
import com.tridion.meta.PublicationMeta;
import com.tridion.meta.PublicationMetaFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MetadataService {

    @Cacheable(value = "conversion", sync = true)
    public PublicationMeta getPublicationMeta(int publicationId) throws ContentProviderException {
        try {
            return new PublicationMetaFactory().getMeta(publicationId);
        } catch (StorageException e) {
            throw new ContentProviderException("Cannot load metadata for publication " + publicationId, e);
        }
    }

    @Cacheable(value = "conversion", sync = true)
    public PageMeta getPageMeta(int publicationId, String pageTcmUri) {
        return new PageMetaFactory(publicationId).getMeta(pageTcmUri);
    }

    @Cacheable(value = "conversion", sync = true)
    public ComponentMeta getComponentMeta(int publicationId, int componentId) {
        return new ComponentMetaFactory(publicationId).getMeta(TcmUtils.buildTcmUri(publicationId, componentId));
    }
}