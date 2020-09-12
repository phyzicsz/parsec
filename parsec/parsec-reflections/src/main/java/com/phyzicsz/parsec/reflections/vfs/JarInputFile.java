package com.phyzicsz.parsec.reflections.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Model for a VFS File.
 *
 * @author phyzicsz (phyzics.z@gmail.com)
 */
public class JarInputFile implements Vfs.File {

    private final ZipEntry entry;
    private final JarInputDir jarInputDir;
    private final long fromIndex;
    private final long endIndex;

    /**
     * Constructor for JarInputFile.
     * 
     * @param entry the zip file entry
     * @param jarInputDir the input directory
     * @param cursor the cursor
     * @param nextCursor the cursor for next
     */
    public JarInputFile(ZipEntry entry, JarInputDir jarInputDir, long cursor, long nextCursor) {
        this.entry = entry;
        this.jarInputDir = jarInputDir;
        fromIndex = cursor;
        endIndex = nextCursor;
    }

    @Override
    public String getName() {
        String name = entry.getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    @Override
    public String getRelativePath() {
        return entry.getName();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (jarInputDir.cursor >= fromIndex && jarInputDir.cursor <= endIndex) {
                    int read = jarInputDir.jarInputStream.read();
                    jarInputDir.cursor++;
                    return read;
                } else {
                    return -1;
                }
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                if (jarInputDir.cursor >= fromIndex && jarInputDir.cursor <= endIndex) {
                    int read = jarInputDir.jarInputStream.read(bytes, offset, length);
                    jarInputDir.cursor++;
                    return read;
                } else {
                    return -1;
                }
            }
        };
    }
}
