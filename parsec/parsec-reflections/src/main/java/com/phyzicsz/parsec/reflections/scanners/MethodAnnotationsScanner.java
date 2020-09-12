package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Store;
import java.util.List;


/**
 * Scans for method's annotations.
 * 
 */
@SuppressWarnings({"unchecked"})
public class MethodAnnotationsScanner extends AbstractScanner {

    @Override
    public void scan(final Object cls, Store store) {
        for (Object method : getMetadataAdapter().getMethods(cls)) {
            for (String methodAnnotation : (List<String>) getMetadataAdapter().getMethodAnnotationNames(method)) {
                if (acceptResult(methodAnnotation)) {
                    put(store, methodAnnotation, getMetadataAdapter().getMethodFullKey(cls, method));
                }
            }
        }
    }
}
