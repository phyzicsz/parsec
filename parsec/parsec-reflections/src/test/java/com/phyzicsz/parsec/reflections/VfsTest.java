package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.adapters.JavassistAdapter;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.vfs.SystemDir;
import com.phyzicsz.parsec.reflections.vfs.Vfs;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import static java.text.MessageFormat.format;
import java.util.Collection;
import javassist.bytecode.ClassFile;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 *
 */
public class VfsTest {

    @Test
    public void testJarFile() throws Exception {
        URL url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", ""));
        assertTrue(url.toString().startsWith("file:"));
        assertTrue(url.toString().contains(".jar"));

        assertTrue(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.jarFile.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarUrl() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(url.toString().startsWith("jar:file:"));
        assertTrue(url.toString().contains(".jar!"));

        assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.jarUrl.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testDirectory() throws Exception {
        URL url = ClasspathHelper.forClass(getClass());
        assertTrue(url.toString().startsWith("file:"));
        assertFalse(url.toString().contains(".jar"));

        assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.directory.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarInputStream() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));
            fail();
        } catch (ReflectionsException e) {
            // expected
        }

        url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", "").replace(".jar!", ".jar"));
        assertTrue(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));

        url = ClasspathHelper.forClass(getClass());
        assertFalse(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void dirWithSpaces() {
        Collection<URL> urls = ClasspathHelper.forPackage("dir+with spaces");
        assertFalse(urls.isEmpty());
        for (URL url : urls) {
            Vfs.Dir dir = Vfs.fromUrl(url);
            Assertions.assertNotNull(dir);
            Assertions.assertNotNull(dir.getFiles().iterator().next());
        }
    }

    @Test
    public void vfsFromDirWithJarInName() throws MalformedURLException {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        tmpFolder = tmpFolder.endsWith(File.separator) ? tmpFolder : tmpFolder + File.separator;
        String dirWithJarInName = tmpFolder + "tony.jarvis";
        File newDir = new File(dirWithJarInName);
        newDir.mkdir();

        try {
            Vfs.Dir dir = Vfs.fromUrl(new URL(format("file:{0}", dirWithJarInName)));

            assertEquals(dirWithJarInName, dir.getPath());
            assertEquals(SystemDir.class, dir.getClass());
        } finally {
            newDir.delete();
        }
    }

    private void testVfsDir(Vfs.Dir dir) {
        JavassistAdapter mdAdapter = new JavassistAdapter();
        Vfs.File file = null;
        for (Vfs.File f : dir.getFiles()) {
            if (f.getRelativePath().endsWith(".class")) {
                file = f;
                break;
            }
        }

        ClassFile stringCF = mdAdapter.getOrCreateClassObject(file);
        String className = mdAdapter.getClassName(stringCF);
        assertFalse(className.isEmpty());
    }
}