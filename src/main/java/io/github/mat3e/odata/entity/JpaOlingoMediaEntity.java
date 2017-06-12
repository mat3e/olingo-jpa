package io.github.mat3e.odata.entity;

import javax.persistence.MappedSuperclass;

/**
 * Special Entity which handles files upload.
 */
@MappedSuperclass
public abstract class JpaOlingoMediaEntity extends JpaOlingoEntity {
    public static final String MEDIA_PROPERTY_NAME = "$value";

    public abstract byte[] getContent();

    public abstract void setContent(byte[] data);

    public abstract String getContentType();

    public abstract void setContentType(String paramString);
}