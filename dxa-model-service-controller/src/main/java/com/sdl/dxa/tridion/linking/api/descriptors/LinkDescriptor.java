package com.sdl.dxa.tridion.linking.api.descriptors;

public interface LinkDescriptor {
    /**
     * @return ID of the publication the links are resolved in.
     */
    default Integer getPageId() {
        return -1;
    };

    /**
     * Describes type of the links this class holds.
     *
     * @return type of the link. Possible values are 'PageLink', 'ComponentLink', 'BinaryLink'
     */
    String getType();

    void setType(String type);

    /**
     * @return ID of the publication the links are resolved in.
     */
    Integer getPublicationId();
}
