package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.adapters.MetadataAdapter;
import com.phyzicsz.parsec.reflections.scanners.Scanner;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * Configuration is used to create a configured instance of {@link Reflections}.
 * 
 * <p>It is preferred to use {@link org.reflections.util.ConfigurationBuilder}
 */
public interface Configuration {

    /**
     * The scanner instances used for scanning different metadata.
     *
     * @return the scanners
     */
    List<Scanner> getScanners();

    /**
     * The urls to be scanned.
     *
     * @return the URLs
     */
    List<URL> getUrls();

    /**
     * The metadata adapter used to fetch metadata from classes.
     *
     * @return the metadata adapter
     */
    MetadataAdapter<?, ?, ?> getMetadataAdapter();

    /**
     * Tet the fully qualified name filter used to filter types to be scanned.
     *
     * @return the input filters
     */
    Predicate<String> getInputsFilter();

    /**
     * Executor service used to scan files.if null, scanning is done in a
     * simple for loop.
     *
     * @return the executor service
     */
    ExecutorService getExecutorService();

    /**
     * Get class loaders, might be used for resolving methods/fields.
     *
     * @return the class loaders
     */
    ClassLoader[] getClassLoaders();

    /**
     * If true (default), expand super types after scanning, for super types
     * that were not scanned.
     * 
     * <p>see {@link org.reflections.Reflections#expandSuperTypes()}
     *
     * @return true if should expand super types
     */
    boolean shouldExpandSuperTypes();
}
