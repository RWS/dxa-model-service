package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.webapp.common.api.content.ContentProviderException;
import com.tridion.broker.StorageException;
import com.tridion.meta.PublicationMeta;
import com.tridion.meta.PublicationMetaFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MetadataService {

    @Cacheable("conversion")
    public PublicationMeta getPublicationMeta(int publicationId) throws ContentProviderException {
        try {
            return new PublicationMetaFactory().getMeta(publicationId);
        } catch (StorageException e) {
            throw new ContentProviderException("Cannot load metadata for publication " + publicationId, e);
        }
    }

}
