package com.phyzicsz.parsec.reflections.util;

import com.phyzicsz.parsec.reflections.util.ClasspathUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test ClasspathHelper utility class
 */
public final class ClasspathUtilsTest {
    @Test
    public void testForClassLoaderShouldntReorderUrls() throws MalformedURLException {
        // testing same URL set with different order to not fall into the case when HashSet orders elements in the same order as we do
        final URL[] urls1 = {new URL("file", "foo", 1111, "foo"), new URL("file", "bar", 1111, "bar"),new URL("file", "baz", 1111, "baz")};
        final List<URL> urlsList2 = Arrays.asList(urls1);
        Collections.reverse(urlsList2);
        final URL[] urls2 = urlsList2.toArray(new URL[urlsList2.size()]);

        final URLClassLoader urlClassLoader1 = new URLClassLoader(urls1, null);
        final URLClassLoader urlClassLoader2 = new URLClassLoader(urls2, null);
        final Collection<URL> resultUrls1 = ClasspathUtils.forClassLoader(urlClassLoader1);
        final Collection<URL> resultUrls2 = ClasspathUtils.forClassLoader(urlClassLoader2);

        Assertions.assertArrayEquals(urls1, resultUrls1.toArray(),"URLs returned from forClassLoader should be in the same order as source URLs");
        Assertions.assertArrayEquals(urls2, resultUrls2.toArray(),"URLs returned from forClassLoader should be in the same order as source URLs");
    }
}
