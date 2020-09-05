package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.Reflections;
import org.junit.Test;
import com.phyzicsz.parsec.reflections.scanners.SubTypesScanner;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class ReflectionsThreadSafenessTest {

    /**
     * https://github.com/ronmamo/reflections/issues/81
     */
    @Test
    public void reflections_scan_is_thread_safe() throws Exception {

        Callable<Set<Class<? extends Logger>>> callable = () -> {
            final Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(singletonList(ClasspathHelper.forClass(Logger.class)))
                    .setScanners(new SubTypesScanner(false)));

            return reflections.getSubTypesOf(Logger.class);
        };

        final ExecutorService pool = Executors.newFixedThreadPool(2);

        final Future<?> first = pool.submit(callable);
        final Future<?> second = pool.submit(callable);

        assertEquals(first.get(5, SECONDS), second.get(5, SECONDS));
    }
}
