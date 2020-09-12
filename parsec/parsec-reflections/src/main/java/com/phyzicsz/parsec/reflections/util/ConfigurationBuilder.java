package com.phyzicsz.parsec.reflections.util;

import com.phyzicsz.parsec.reflections.Configuration;
import com.phyzicsz.parsec.reflections.ReflectionsException;
import com.phyzicsz.parsec.reflections.adapters.JavaReflectionAdapter;
import com.phyzicsz.parsec.reflections.adapters.JavassistAdapter;
import com.phyzicsz.parsec.reflections.adapters.MetadataAdapter;
import com.phyzicsz.parsec.reflections.scanners.Scanner;
import com.phyzicsz.parsec.reflections.scanners.SubTypesScanner;
import com.phyzicsz.parsec.reflections.scanners.TypeAnnotationsScanner;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fluent builder for {@link org.reflections.Configuration}, to be used for
 * constructing a {@link com.phyzicsz.parsec.reflections.Reflections} instance.
 * 
 * <p>Usage:
 * <pre>{@code
 *      new Reflections(
 *          new ConfigurationBuilder()
 *              .filterInputsBy(new FilterBuilder().include("your project's common package prefix here..."))
 *              .setUrls(ClasspathHelper.forClassLoader())
 *              .setScanners(new SubTypesScanner(), 
 *                  new TypeAnnotationsScanner()
 *                      .filterResultsBy(myClassAnnotationsFilter)));
 * }
 * </pre>
 * <br>{@link #executorService} is used optionally used for parallel scanning.
 * if value is null then scanning is done in a simple for loop
 * 
 * <p>defaults: accept all for {@link #inputsFilter}
 */
public class ConfigurationBuilder implements Configuration {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);

    private final List<Scanner> scanners;
    private List<URL> urls;
    /*lazy*/ protected MetadataAdapter<?, ?, ?> metadataAdapter;
    private Predicate<String> inputsFilter;
    private ExecutorService executorService;
    private ClassLoader[] classLoaders;
    private boolean expandSuperTypes = true;

    public ConfigurationBuilder() {
        scanners = new ArrayList<>(Arrays.asList(new TypeAnnotationsScanner(), new SubTypesScanner()));
        urls = new ArrayList<>();
    }

    /**
     * Constructs a {@link ConfigurationBuilder} using the given parameters,
     * in a non statically typed way.that is, each element in {@code params} is
     * guessed by it's type and populated into the configuration.
     *
     * <ul>
     * <li>{@link String} - add urls using
     * {@link ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
     * <li>{@link Class} - add urls using
     * {@link ClasspathHelper#forClass(Class, ClassLoader...)} </li>
     * <li>{@link ClassLoader} - use these classloaders in order to find urls in
     * ClasspathHelper.forPackage(), ClasspathHelper.forClass() and for
     * resolving types</li>
     * <li>{@link Scanner} - use given scanner, overriding the default
     * scanners</li>
     * <li>{@link URL} - add the given url for scanning</li>
     * <li>{@code Object[]} - flatten and use each element as above</li>
     * </ul>
     *
     * <p>An input {@link FilterBuilder} will be set according to given packages.
     * 
     * <p>Use any parameter type in any order. this constructor uses instanceof on
     * each param and instantiate a {@link ConfigurationBuilder} appropriately.
     *
     * @param params configuration parameters
     * @return fluent builder
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigurationBuilder build(final Object... params) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        //flatten
        List<Object> parameters = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                if (param != null) {
                    if (param.getClass().isArray()) {
                        for (Object p : (Object[]) param) {
                            if (p != null) {
                                parameters.add(p);
                            }
                        }
                    } else if (param instanceof Iterable) {
                        for (Object p : (Iterable) param) {
                            if (p != null) {
                                parameters.add(p);
                            }
                        }
                    } else {
                        parameters.add(param);
                    }
                }
            }
        }

        List<ClassLoader> loaders = new ArrayList<>();
        for (Object param : parameters) {
            if (param instanceof ClassLoader) {
                loaders.add((ClassLoader) param);
            }
        }

        ClassLoader[] classLoaders = loaders.isEmpty() ? null : loaders.toArray(new ClassLoader[loaders.size()]);
        FilterBuilder filter = new FilterBuilder();
        List<Scanner> scanners = new ArrayList<>();

        for (Object param : parameters) {
            if (param instanceof String) {
                builder.addUrls(ClasspathHelper.forPackage((String) param, classLoaders));
                filter.includePackage((String) param);
            } else if (param instanceof Class) {
                if (Scanner.class.isAssignableFrom((Class) param)) {
                    try {
                        builder.addScanners(((Scanner) ((Class<?>) param).getDeclaredConstructor().newInstance()));
                    } catch (IllegalAccessException 
                            | InstantiationException 
                            | NoSuchMethodException 
                            | SecurityException 
                            | IllegalArgumentException 
                            | InvocationTargetException ex) {
                        //fallback
                    }
                }
                builder.addUrls(ClasspathHelper.forClass((Class) param, classLoaders));
                filter.includePackage(((Class) param));
            } else if (param instanceof Scanner) {
                scanners.add((Scanner) param);
            } else if (param instanceof URL) {
                builder.addUrls((URL) param);
            } else if (param instanceof ClassLoader) {
                /* already taken care */
            } else if (param instanceof Predicate) {
                filter.add((Predicate<String>) param);
            } else if (param instanceof ExecutorService) {
                builder.setExecutorService((ExecutorService) param);
            } else {
                throw new ReflectionsException("could not use param " + param);
            }
        }

        if (builder.getUrls().isEmpty()) {
            if (classLoaders != null) {
                builder.addUrls(ClasspathHelper.forClassLoader(classLoaders)); //default urls getResources("")
            } else {
                builder.addUrls(ClasspathHelper.forClassLoader()); //default urls getResources("")
            }
            if (builder.urls.isEmpty()) {
                builder.addUrls(ClasspathHelper.forJavaClassPath());
            }
        }

        builder.filterInputsBy(filter);
        if (!scanners.isEmpty()) {
            builder.setScanners(scanners.toArray(new Scanner[scanners.size()]));
        }
        if (!loaders.isEmpty()) {
            builder.addClassLoaders(loaders);
        }

        return builder;
    }

    /**
     * Builder for the given packages.
     * 
     * @param packages the packages to scan
     * @return fluent builder
     */
    public ConfigurationBuilder forPackages(String... packages) {
        for (String pkg : packages) {
            addUrls(ClasspathHelper.forPackage(pkg));
        }
        return this;
    }

    @Override
    public List<Scanner> getScanners() {
        return scanners;
    }

    /**
     * Set the scanners instances for scanning different metadata.
     *
     * @param scanners the scanners
     * @return fluent builder
     */
    public ConfigurationBuilder setScanners(final Scanner... scanners) {
        this.scanners.clear();
        return addScanners(scanners);
    }

    /**
     * Set the scanners instances for scanning different metadata.
     *
     * @param scanners the scanners
     * @return fluent builder
     */
    public ConfigurationBuilder addScanners(final Scanner... scanners) {
        this.scanners.addAll(Arrays.asList(scanners));
        return this;
    }

    @Override
    public List<URL> getUrls() {
        return urls;
    }

    /**
     * Set the urls to be scanned.
     *
     * <p>Use {@link com.phyzicsz.parsec.reflections.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     * @param urls the urls
     * @return fluent builder
     */
    public ConfigurationBuilder setUrls(final Collection<URL> urls) {
        this.urls = new ArrayList<>(urls);
        return this;
    }

    /**
     * Set the urls to be scanned.
     *
     * <p>Use {@link com.phyzicsz.parsec.reflections.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     * @param urls the urls
     * @return fluent builder
     */
    public ConfigurationBuilder setUrls(final URL... urls) {
        this.urls = new ArrayList<>(Arrays.asList(urls));
        return this;
    }

    /**
     * Add urls to be scanned.
     *
     * <p>Use {@link com.phyzicsz.parsec.reflections.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     * @param urls the urls
     * @return fluent builder
     */
    public ConfigurationBuilder addUrls(final Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    /**
     * Add urls to be scanned.
     * 
     * <p>Use {@link com.phyzicsz.parsec.reflections.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     * @param urls the urls
     * @return fluent builder
     */
    public ConfigurationBuilder addUrls(final URL... urls) {
        this.urls.addAll(new ArrayList<>(Arrays.asList(urls)));
        return this;
    }

    /**
     * Returns the metadata adapter. if javassist library exists in the
     * classpath, this method returns {@link JavassistAdapter} otherwise
     * defaults to {@link JavaReflectionAdapter}.
     *
     * <p>The {@link JavassistAdapter} is preferred in terms of performance and
     * class loading.
     *
     * @return the metadata adapter
     */
    @Override
    public MetadataAdapter<?, ?, ?> getMetadataAdapter() {
        if (metadataAdapter != null) {
            return metadataAdapter;
        } else {
            try {
                return (metadataAdapter = new JavassistAdapter());
            } catch (Throwable e) {
                logger.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);
                return (metadataAdapter = new JavaReflectionAdapter());
            }
        }
    }

    /**
     * Sets the metadata adapter used to fetch metadata from classes.
     *
     * @param metadataAdapter the metadata adapter to use
     * @return fluent builder
     */
    public ConfigurationBuilder setMetadataAdapter(final MetadataAdapter<?, ?, ?> metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
        return this;
    }

    @Override
    public Predicate<String> getInputsFilter() {
        return inputsFilter;
    }

    /**
     * Sets the input filter for all resources to be scanned.
     * 
     * <p>Supply a {@link Predicate} or use the {@link FilterBuilder}
     *
     * @param inputsFilter the input filter
     */
    public void setInputsFilter(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
    }

    /**
     * Sets the input filter for all resources to be scanned.
     *
     * <p>Supply a {@link Predicate} or use the {@link FilterBuilder}
     *
     * @param inputsFilter the input filter
     * @return fluent builder
     */
    public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Sets the executor service used for scanning.
     *
     * @param executorService the executor service to use
     * @return fluent builder
     */
    public ConfigurationBuilder setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Sets the executor service used for scanning to ThreadPoolExecutor with
     * core size as {@link java.lang.Runtime#availableProcessors()}.
     *
     * <p>Default is ThreadPoolExecutor with a single core
     *
     * @return fluent builde
     */
    public ConfigurationBuilder useParallelExecutor() {
        return useParallelExecutor(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Sets the executor service used for scanning to ThreadPoolExecutor with
     * core size as the given availableProcessors parameter.
     *
     * <p>The executor service spawns daemon threads by default.
     *
     * <p>Default is ThreadPoolExecutor with a single core
     *
     * @param availableProcessors the number of available processors
     * @return fluent builder
     */
    public ConfigurationBuilder useParallelExecutor(final int availableProcessors) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("org.reflections-scanner-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        setExecutorService(Executors.newFixedThreadPool(availableProcessors, threadFactory));
        return this;
    }

    /**
     * Get class loader, might be used for scanning or resolving methods/fields.
     *
     * @return array of class loaders
     */
    @Override
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }

    @Override
    public boolean shouldExpandSuperTypes() {
        return expandSuperTypes;
    }

    /**
     * If set to true, Reflections will expand super types after scanning.
     *
     * <p>See {@link org.reflections.Reflections#expandSuperTypes()}
     *
     * @param expandSuperTypes flag to expand supertypes (or not)
     * @return fluent builder
     */
    public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
        this.expandSuperTypes = expandSuperTypes;
        return this;
    }

    /**
     * Set class loader, might be used for resolving methods/fields.
     *
     * @param classLoaders the class loaders
     * @return fluent builder
     */
    public ConfigurationBuilder setClassLoaders(ClassLoader[] classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /**
     * Add class loader, might be used for resolving methods/fields.
     *
     * @param classLoader the class loader
     * @return fluent builder
     */
    public ConfigurationBuilder addClassLoader(ClassLoader classLoader) {
        return addClassLoaders(classLoader);
    }

    /**
     * Add class loader, might be used for resolving methods/fields.
     *
     * @param classLoaders the class loaders
     * @return fluent builder
     */
    public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = this.classLoaders == null
                ? classLoaders
                : Stream.concat(Arrays.stream(this.classLoaders), 
                        Arrays.stream(classLoaders)).toArray(ClassLoader[]::new);
        return this;
    }

    /**
     * Add class loader, might be used for resolving methods/fields.
     *
     * @param classLoaders the class loaders
     * @return fluent builder
     */
    public ConfigurationBuilder addClassLoaders(Collection<ClassLoader> classLoaders) {
        return addClassLoaders(classLoaders.toArray(new ClassLoader[classLoaders.size()]));
    }
}
