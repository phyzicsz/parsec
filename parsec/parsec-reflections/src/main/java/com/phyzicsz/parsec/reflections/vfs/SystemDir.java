package com.phyzicsz.parsec.reflections.vfs;

import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link org.reflections.vfs.Vfs.Dir} for directory {@link java.io.File}.
 * 
 * @author phyzicsz (phyzics.z@gmail.com)
 */
public class SystemDir implements Vfs.Dir {

    private final File file;

    /**
     * Constructor for SystemDir.
     * 
     * @param file the input directory
     */
    public SystemDir(File file) {
        if (file != null && (!file.isDirectory() || !file.canRead())) {
            throw new RuntimeException("cannot use dir " + file);
        }

        this.file = file;
    }

    @Override
    public String getPath() {
        if (file == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return file.getPath().replace("\\", "/");
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        if (file == null || !file.exists()) {
            return Collections.emptyList();
        }
        
        Iterable<Vfs.File> iterable = null;
        try (Stream<Path> stream = Files.walk(file.toPath())) {
            iterable = stream.filter(Files::isRegularFile)
                        .map(path -> (Vfs.File) new SystemFile(SystemDir.this, path.toFile()))
                        .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new ReflectionsException("could not get files for " + file, ex);
        }

        return iterable;
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return getPath();
    }
}
