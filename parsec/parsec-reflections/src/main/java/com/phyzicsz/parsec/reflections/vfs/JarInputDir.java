package com.phyzicsz.parsec.reflections.vfs;

import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import com.phyzicsz.parsec.reflections.util.Utils;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * Model for an VFS directory for a jar file.
 * 
 * @author phyzicsz (phyzics.z@gmail.com)
 */
public class JarInputDir implements Vfs.Dir {

    private final URL url;
    JarInputStream jarInputStream;
    long cursor = 0;
    long nextCursor = 0;

    public JarInputDir(URL url) {
        this.url = url;
    }

    @Override
    public String getPath() {
        return url.getPath();
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> new Iterator<Vfs.File>() {

            {
                try {
                    jarInputStream = new JarInputStream(url.openConnection().getInputStream());
                } catch (IOException e) {
                    throw new ReflectionsException("Could not open url connection", e);
                }
            }

            Vfs.File entry = null;

            @Override
            public boolean hasNext() {
                return entry != null || (entry = computeNext()) != null;
            }

            @Override
            public Vfs.File next() {
                Vfs.File next = entry;
                entry = null;
                return next;
            }

            private Vfs.File computeNext() {
                while (true) {
                    try {
                        ZipEntry entry = jarInputStream.getNextJarEntry();
                        if (entry == null) {
                            return null;
                        }

                        long size = entry.getSize();
                        if (size < 0) {
                            size = 0xffffffffL + size; //JDK-6916399
                        }
                        nextCursor += size;
                        if (!entry.isDirectory()) {
                            return new JarInputFile(entry, JarInputDir.this, cursor, nextCursor);
                        }
                    } catch (IOException e) {
                        throw new ReflectionsException("could not get next zip entry", e);
                    }
                }
            }
        };
    }

    @Override
    public void close() {
        Utils.close(jarInputStream);
    }
}
