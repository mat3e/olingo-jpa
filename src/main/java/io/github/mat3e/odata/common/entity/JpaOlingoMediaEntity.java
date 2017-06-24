package io.github.mat3e.odata.common.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Special Entity which handles files upload.
 */
@MappedSuperclass
public abstract class JpaOlingoMediaEntity extends JpaOlingoEntity {
    public static final String MEDIA_PROPERTY_NAME = "$value";

    @Column(name = "content_type")
    protected String contentType;

    public abstract byte[] getContent();

    public abstract void setContent(byte[] data);

    @Override
    public String getMediaContentType() {
        String result = super.getMediaContentType();
        if (result == null) {
            result = contentType;
            super.setMediaContentType(result);
        }

        return result;
    }

    @Override
    public void setMediaContentType(String mediaType) {
        contentType = mediaType;
        super.setMediaContentType(mediaType);
    }
}