package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.configuration.Configuration;
import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import com.phyzicsz.parsec.reflections.scanners.Scanner;
import com.phyzicsz.parsec.reflections.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stores metadata information in multimaps.
 * 
 * <p>Use the different query methods (getXXX) to query the metadata
 * 
 * <p>The query methods are string based, and does not cause the class loader to
 * define the types
 * 
 * <p>Use {@link org.reflections.Reflections#getStore()} to access this store
 */
public class Store {

    private final ConcurrentHashMap<String, Map<String, Collection<String>>> storeMap;

    protected Store(Configuration configuration) {
        storeMap = new ConcurrentHashMap<>();
        for (Scanner scanner : configuration.getScanners()) {
            String index = Utils.index(scanner.getClass());
            storeMap.computeIfAbsent(index, s -> new ConcurrentHashMap<>());
        }
    }

    /**
     * Return all indices.
     *
     * @return the keys in the store
     */
    public Set<String> keySet() {
        return storeMap.keySet();
    }

    /**
     * Get the multimap object for the given {@code index}, otherwise throws a
     * {@link com.phyzicsz.parsec.reflections.ReflectionsException}.
     *
     */
    private Map<String, Collection<String>> get(String index) {
        Map<String, Collection<String>> mmap = storeMap.get(index);
        if (mmap == null) {
            throw new ReflectionsException("Scanner " + index + " was not configured");
        }
        return mmap;
    }

    /**
     * Get the values stored for the given {@code index} and {@code key}.
     *
     * @param scannerClass the scanner class
     * @param key the key
     * @return the matching values
     */
    public Set<String> get(Class<?> scannerClass, String key) {
        return get(Utils.index(scannerClass), Collections.singletonList(key));
    }

    /**
     * Get the values stored for the given {@code index} and {@code key}.
     *
     * @param index the index
     * @param key the key
     * @return the matching values
     */
    public Set<String> get(String index, String key) {
        return get(index, Collections.singletonList(key));
    }

    /**
     * Get the values stored for the given {@code index} and {@code keys}.
     *
     * @param scannerClass the scanner class
     * @param keys the keys
     * @return the matching values
     */
    public Set<String> get(Class<?> scannerClass, Collection<String> keys) {
        return get(Utils.index(scannerClass), keys);
    }

    /**
     * Get the values stored for the given {@code index} and {@code keys}.
     *
     */
    private Set<String> get(String index, Collection<String> keys) {
        Map<String, Collection<String>> mmap = get(index);
        Set<String> result = new LinkedHashSet<>();
        for (String key : keys) {
            Collection<String> values = mmap.get(key);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }

    /**
     * Recursively get the values stored for the given {@code index} and
     * {@code keys}, including keys.
     *
     * @param scannerClass the scanner class
     * @param keys the keys
     * @return the matching values
     */
    public Set<String> getAllIncluding(Class<?> scannerClass, Collection<String> keys) {
        String index = Utils.index(scannerClass);
        Map<String, Collection<String>> mmap = get(index);
        List<String> workKeys = new ArrayList<>(keys);

        Set<String> result = new HashSet<>();
        for (int i = 0; i < workKeys.size(); i++) {
            String key = workKeys.get(i);
            if (result.add(key)) {
                Collection<String> values = mmap.get(key);
                if (values != null) {
                    workKeys.addAll(values);
                }
            }
        }
        return result;
    }

    /**
     * Recursively get the values stored for the given {@code index} and
     * {@code key}, not including keys.
     *
     * @param scannerClass the scanner class
     * @param key the key
     * @return the matching values
     */
    public Set<String> getAll(Class<?> scannerClass, String key) {
        return getAllIncluding(scannerClass, get(scannerClass, key));
    }

    /**
     * Recursively get the values stored for the given {@code index} and
     * {@code keys}, not including keys.
     *
     * @param scannerClass the scanner class
     * @param keys the keys
     * @return the matching values
     */
    public Set<String> getAll(Class<?> scannerClass, Collection<String> keys) {
        return getAllIncluding(scannerClass, get(scannerClass, keys));
    }

    public Set<String> keys(String index) {
        Map<String, Collection<String>> map = storeMap.get(index);
        return map != null ? new HashSet<>(map.keySet()) : Collections.emptySet();
    }

    /**
     * Get the values for the index.
     * 
     * @param index the index
     * @return the set of values
     */
    public Set<String> values(String index) {
        Map<String, Collection<String>> map = storeMap.get(index);
        return map != null ? map
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()) : Collections.emptySet();
    }

    
    public boolean put(Class<?> scannerClass, String key, String value) {
        return put(Utils.index(scannerClass), key, value);
    }

    /**
     * Store a value for an index and key.
     * 
     * @param index the index to store the value in
     * @param key the key to store the value with
     * @param value the value to store
     * @return boolean true if stored
     */
    public boolean put(String index, String key, String value) {
        return storeMap.computeIfAbsent(index, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, s -> Collections.synchronizedList(new ArrayList<>()))
                .add(value);
    }

    void merge(Store store) {
        if (store != null) {
            for (String indexName : store.keySet()) {
                Map<String, Collection<String>> index = store.get(indexName);
                if (index != null) {
                    for (String key : index.keySet()) {
                        for (String string : index.get(key)) {
                            put(indexName, key, string);
                        }
                    }
                }
            }
        }
    }
}
