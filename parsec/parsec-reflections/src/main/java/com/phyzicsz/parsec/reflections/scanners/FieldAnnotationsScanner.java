package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Store;
import java.util.List;

/**
 * Scans for field's annotations.
 * 
 */
@SuppressWarnings({"unchecked"})
public class FieldAnnotationsScanner extends AbstractScanner {

    @Override
    public void scan(final Object cls, Store store) {
        final String className = getMetadataAdapter().getClassName(cls);
        List<Object> fields = getMetadataAdapter().getFields(cls);
        for (final Object field : fields) {
            List<String> fieldAnnotations = getMetadataAdapter().getFieldAnnotationNames(field);
            for (String fieldAnnotation : fieldAnnotations) {

                if (acceptResult(fieldAnnotation)) {
                    String fieldName = getMetadataAdapter().getFieldName(field);
                    put(store, fieldAnnotation, String.format("%s.%s", className, fieldName));
                }
            }
        }
    }
}
