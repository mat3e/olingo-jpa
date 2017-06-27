package io.github.mat3e.odata.common.util;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import io.github.mat3e.odata.common.exception.CsdlExtractException;

/**
 * Util for mapping from Java to Edm types.
 */
public class EdmTypeUtil {

    private static final Map<Class<?>, EdmPrimitiveTypeKind> types = new HashMap<Class<?>, EdmPrimitiveTypeKind>() {{
        put(String.class, EdmPrimitiveTypeKind.String);
        put(Integer.class, EdmPrimitiveTypeKind.Int32);
        put(Integer.TYPE, EdmPrimitiveTypeKind.Int32);
        put(Long.class, EdmPrimitiveTypeKind.Int64);
        put(Long.TYPE, EdmPrimitiveTypeKind.Int64);
        put(Double.class, EdmPrimitiveTypeKind.Double);
        put(Double.TYPE, EdmPrimitiveTypeKind.Double);
        put(Boolean.class, EdmPrimitiveTypeKind.Boolean);
        put(Boolean.TYPE, EdmPrimitiveTypeKind.Boolean);
        put(Float.class, EdmPrimitiveTypeKind.Single);
        put(Float.TYPE, EdmPrimitiveTypeKind.Single);
        put(Short.class, EdmPrimitiveTypeKind.Int16);
        put(Short.TYPE, EdmPrimitiveTypeKind.Int16);
        put(BigDecimal.class, EdmPrimitiveTypeKind.Decimal);
        put(Calendar.class, EdmPrimitiveTypeKind.Date);
        put(Date.class, EdmPrimitiveTypeKind.Date);
        put(LocalDate.class, EdmPrimitiveTypeKind.Date);
        put(Timestamp.class, EdmPrimitiveTypeKind.DateTimeOffset);
        put(LocalDateTime.class, EdmPrimitiveTypeKind.DateTimeOffset);
        put(byte[].class, EdmPrimitiveTypeKind.Binary);
        put(Byte[].class, EdmPrimitiveTypeKind.Binary);
        put(Blob.class, EdmPrimitiveTypeKind.Binary);
        put(Byte.class, EdmPrimitiveTypeKind.SByte);
        put(Byte.TYPE, EdmPrimitiveTypeKind.SByte);
        put(LocalTime.class, EdmPrimitiveTypeKind.TimeOfDay);
        put(Time.class, EdmPrimitiveTypeKind.TimeOfDay);
        put(Duration.class, EdmPrimitiveTypeKind.Duration);
        put(UUID.class, EdmPrimitiveTypeKind.Guid);
        put(Clob.class, EdmPrimitiveTypeKind.String);
    }};

    /**
     * Returns EDM type for Java class.
     *
     * @param clazz
     *         Java class for being converted
     * @return EDM representation of type or supertype
     * @throws CsdlExtractException
     *         when no match possible
     */
    public static EdmPrimitiveTypeKind getEdmPrimitiveTypeFor(Class<?> clazz) throws CsdlExtractException {
        EdmPrimitiveTypeKind result = types.get(clazz);
        if (result == null) {
            Optional<Class<?>> potentialType = types.keySet().stream()
                                                    .filter(javaClass -> javaClass.isAssignableFrom(clazz)).findFirst();
            if (potentialType.isPresent()) {
                result = types.get(potentialType.get());
            } else {
                throw new CsdlExtractException("Property of type " + clazz + " cannot be mapped to OData type");
            }
        }

        return result;
    }
}
