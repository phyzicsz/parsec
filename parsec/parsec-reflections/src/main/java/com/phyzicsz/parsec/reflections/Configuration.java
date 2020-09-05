package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.adapters.MetadataAdapter;
import com.phyzicsz.parsec.reflections.scanners.Scanner;
import com.phyzicsz.parsec.reflections.serializers.Serializer;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>
 * it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
 */
public interface Configuration {

    /**
     * the scanner instances used for scanning different metadata
     *
     * @return
     */
    Set<Scanner> getScanners();

    /**
     * the urls to be scanned
     *
     * @return
     */
    Set<URL> getUrls();

    /**
     * the metadata adapter used to fetch metadata from classes
     *
     * @return
     */
    MetadataAdapter<?,?,?> getMetadataAdapter();

    /**
     * get the fully qualified name filter used to filter types to be scanned
     *
     * @return
     */
    Predicate<String> getInputsFilter();

    /**
     * * executor service used to scan files.if null, scanning is done in a
     * simple for loop
     *
     * @return
     */
    ExecutorService getExecutorService();

    /**
     * the default serializer to use when saving Reflection
     *
     * @return
     */
    Serializer getSerializer();

    /**
     * get class loaders, might be used for resolving methods/fields
     *
     * @return
     */
    ClassLoader[] getClassLoaders();

    /**
     * if true (default), expand super types after scanning, for super types
     * that were not scanned.
     * <p>
     * see {@link org.reflections.Reflections#expandSuperTypes()
     *
     * @return }
     */
    boolean shouldExpandSuperTypes();
}
