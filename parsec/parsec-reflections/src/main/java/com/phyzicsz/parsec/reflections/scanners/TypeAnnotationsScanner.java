package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Store;
import java.lang.annotation.Inherited;
import java.util.List;

/**
 * Scans for class's annotations, where @Retention(RetentionPolicy.RUNTIME).
 * 
 */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner {

    @Override
    public void scan(final Object cls, Store store) {
        final String className = getMetadataAdapter().getClassName(cls);

        for (String annotationType : (List<String>) getMetadataAdapter().getClassAnnotationNames(cls)) {

            if (acceptResult(annotationType)
                    || annotationType.equals(Inherited.class.getName())) { //as an exception, accept Inherited as well
                put(store, annotationType, className);
            }
        }
    }

}
