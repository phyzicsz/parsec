package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Store;
import com.phyzicsz.parsec.reflections.filter.FilterBuilder;
import java.util.List;

/**
 * Scans for superclass and interfaces of a class, allowing a reverse lookup for
 * subtypes.
 * 
 */
public class SubTypesScanner extends AbstractScanner {

    /**
     * Created new SubTypesScanner. will exclude direct Object subtypes.
     * 
     */
    public SubTypesScanner() {
        this(true); //exclude direct Object subtypes by default
    }

    /**
     * Created new SubTypesScanner.
     *
     * @param excludeObjectClass if false, include direct {@link Object} subtypes in results.
     */
    public SubTypesScanner(boolean excludeObjectClass) {
        if (excludeObjectClass) {
            filterResultsBy(new FilterBuilder().exclude(Object.class.getName())); //exclude direct Object subtypes
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void scan(final Object cls, Store store) {
        String className = getMetadataAdapter().getClassName(cls);
        String superclass = getMetadataAdapter().getSuperclassName(cls);

        if (acceptResult(superclass)) {
            put(store, superclass, className);
        }

        for (String anInterface : (List<String>) getMetadataAdapter().getInterfacesNames(cls)) {
            if (acceptResult(anInterface)) {
                put(store, anInterface, className);
            }
        }
    }
}
