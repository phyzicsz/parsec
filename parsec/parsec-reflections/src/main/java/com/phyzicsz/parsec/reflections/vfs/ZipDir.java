package com.phyzicsz.parsec.reflections.vfs;

import java.io.IOException;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link org.reflections.vfs.Vfs.Dir} for
 * {@link java.util.zip.ZipFile}.
 * 
 * @author phyzicsz (phyzics.z@gmail.com)
 */
public class ZipDir implements Vfs.Dir {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipDir.class);

    final java.util.zip.ZipFile jarFile;

    public ZipDir(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public String getPath() {
        if (jarFile == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return jarFile.getName().replace("\\", "/");
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .map(entry -> (Vfs.File) new ZipFile(ZipDir.this, entry))
                .iterator();
    }

    @Override
    public void close() {
        try {
            jarFile.close();
        } catch (IOException e) {
            logger.warn("Could not close JarFile", e);
        }
    }

    @Override
    public String toString() {
        return jarFile.getName();
    }
}
