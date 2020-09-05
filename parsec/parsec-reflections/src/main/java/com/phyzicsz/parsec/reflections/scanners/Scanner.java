package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Configuration;
import com.phyzicsz.parsec.reflections.Store;
import com.phyzicsz.parsec.reflections.vfs.Vfs;
import java.util.function.Predicate;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file, Object classObject, Store store);

    boolean acceptResult(String fqn);
}
