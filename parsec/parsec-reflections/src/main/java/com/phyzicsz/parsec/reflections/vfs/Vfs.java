package com.phyzicsz.parsec.reflections.vfs;

import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import com.phyzicsz.parsec.reflections.util.ClasspathUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple virtual file system bridge.
 *
 * <p>Use the {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} to get a
 * {@link com.phyzics.parser.reflections.vfs.Vfs.Dir}, then use
 * {@link com.phyzics.parser.reflections.vfs.Vfs.Dir#getFiles()} to iterate over
 * the {@link com.phyzics.parser.reflections.vfs.Vfs.File}
 *
 * <p>For example:
 * <pre>{@code
 *      Vfs.Dir dir = Vfs.fromURL(url);
 *      Iterable<Vfs.File> files = dir.getFiles();
 *      for (Vfs.File file : files) {
 *          InputStream is = file.openInputStream();
 *      }
 * }
 * </pre>
 * 
 * <p>{@link com.phyzics.parser.reflections.vfs.Vfs#fromURL(java.net.URL)} uses
 * static {@link com.phyzics.parser.reflections.vfs.Vfs.DefaultUrlTypes} to
 * resolve URLs. It contains VfsTypes for handling for common resources such as
 * local jar file, local directory, jar url, jar input stream and more.
 *
 * <p>It can be plugged in with other {@link org.reflections.vfs.Vfs.UrlType} using
 * {@link com.phyzics.parser.reflections.vfs.Vfs#addDefaultURLTypes(org.reflections.vfs.Vfs.UrlType)}
 * or
 * {@link com.phyzics.parser.reflections.vfs.Vfs#setDefaultURLTypes(java.util.List)}.
 *
 * <p>For example:
 * <pre>
 *      Vfs.addDefaultURLTypes(new Vfs.UrlType() {
 *          public boolean matches(URL url)         {
 *              return url.getProtocol().equals("http");
 *          }
 *          public Vfs.Dir createDir(final URL url) {
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      Vfs.Dir dir = Vfs.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </pre>
 * 
 * <p>Use
 * {@link com.phyzics.parser.reflections.vfs.Vfs#findFiles(java.util.Collection, java.util.function.Predicate)}
 * to get an iteration of files matching given name predicate over given list of
 * urls
 */
public abstract class Vfs {

    private static final Logger logger = LoggerFactory.getLogger(Vfs.class);

    private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));

    /**
     * An abstract vfs directory.
     * 
     */
    public interface Dir {

        String getPath();

        Iterable<File> getFiles();

        void close();
    }

    /**
     * An abstract vfs file.
     */
    public interface File {

        String getName();

        String getRelativePath();

        InputStream openInputStream() throws IOException;
    }

    /**
     * a matcher and factory for a url.
     * 
     */
    public interface UrlType {

        boolean matches(URL url) throws Exception;

        Dir createDir(URL url) throws Exception;
    }

    /**
     * The default url types that will be used when issuing
     * {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)}.
     *
     * @return a List of UrlType
     */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /**
     * Sets the static default url types.
     * 
     * <p>Can be used to statically plug in urlTypes
     *
     * @param urlTypes the static default URLType's
     */
    public static void setDefaultUrlTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /**
     * Add a static default url types to the beginning of the default url types
     * list.can be used to statically plug in urlTypes.
     *
     * @param urlType the default URLType
     */
    public static void addDefaultUrlTypes(UrlType urlType) {
        defaultUrlTypes.add(0, urlType);
    }

    /**
     * Create a Dir from the given url, using the defaultUrlTypes.
     *
     * @param url the URL to create the Dir from
     * @return the created Dir
     */
    public static Dir fromUrl(final URL url) {
        return fromUrl(url, defaultUrlTypes);
    }

    /**
     * Create a Dir from the given url, using the given urlType.
     *
     * @param url      the URL
     * @param urlTypes the URLTYpe
     * @return the created Dir
     */
    public static Dir fromUrl(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            try {
                if (type.matches(url)) {
                    Dir dir = type.createDir(url);
                    if (dir != null) {
                        return dir;
                    }
                }
            } catch (Exception e) {
                logger.warn("could not create Dir using {} from url {}", type, url.toExternalForm(), e);
            }
        }

        throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" 
                + url.toExternalForm() 
                + "]\n"
                + "either use fromURL(final URL url, final List<UrlType> urlTypes) or "
                + "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or "
                + "addDefaultURLTypes(UrlType urlType) "
                + "with your specialized UrlType.");
    }

    /**
     * Create a Dir from the given url, using the given urlType.
     *
     * @param url      the url path
     * @param urlTypes The URLType
     * @return the created Dir
     */
    public static Dir fromUrl(final URL url, final UrlType... urlTypes) {
        return fromUrl(url, Arrays.asList(urlTypes));
    }

    /**
     * Return an iterable of all
     * {@link com.phyzics.parser.reflections.vfs.Vfs.File} in given urls,
     * starting with given packagePrefix and matching nameFilter.
     *
     * @param inUrls        collection of URLs
     * @param packagePrefix the package prefix
     * @param nameFilter    a mathing filter
     * @return an iterable of Files
     */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, 
            final String packagePrefix, 
            final Predicate<String> nameFilter) {
        
        Predicate<File> fileNamePredicate = file -> {
            String path = file.getRelativePath();
            if (path.startsWith(packagePrefix)) {
                String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                return !filename.isEmpty() && nameFilter.test(filename.substring(1));
            } else {
                return false;
            }
        };

        return findFiles(inUrls, fileNamePredicate);
    }

    /**
     * Return an iterable of all
     * {@link com.phyzics.parser.reflections.vfs.Vfs.File} in given urls,
     * matching filePredicate.
     *
     * @param urls          a colleciton of URL paths
     * @param filePredicate a mathing predicate
     * @return an iterable of Files
     */
    public static Iterable<File> findFiles(final Collection<URL> urls, final Predicate<File> filePredicate) {
        return () -> urls.stream()
                .flatMap(url -> {
                    try {
                        return StreamSupport.stream(fromUrl(url).getFiles().spliterator(), false);
                    } catch (Throwable e) {
                        logger.error("could not findFiles for url: {}", url, e);
                        return Stream.of();
                    }
                })
                .filter(filePredicate).iterator();
    }

    /**
     * Get {@link java.io.File} from url.
     *
     * @param url the input URL
     * @return a File for the URL
     */
    public static java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }
        } catch (URISyntaxException ignored) {
            logger.debug("could not create the File from the URL...trying decoding");
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) {
                path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (UnsupportedEncodingException ignored) {
            logger.debug("could not create the File from the decoded URL...trying other methods");
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) {
                path = path.substring("jar:".length());
            }
            if (path.startsWith("wsjar:")) {
                path = path.substring("wsjar:".length());
            }
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }
            if (path.contains(".jar!")) {
                path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            }
            if (path.contains(".war!")) {
                path = path.substring(0, path.indexOf(".war!") + ".war".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (Exception ignored) {
            logger.debug("could not create the File from the URL - returning null");
        }

        return null;
    }

    private static boolean hasJarFileInPath(URL url) {
        return url.toExternalForm().matches(".*\\.jar(\\!.*|$)");
    }

    /**
     * Default url types used by
     * {@link com.phyzics.parser.reflections.vfs.Vfs#fromURL(java.net.URL)}.
     * 
     * <p>jarFile - creates a {@link com.phyzicsz.parsec.reflections.vfs.ZipDir}
     * over jar file
     * 
     * <p>jarUrl - creates a {@link com.phyzicsz.parsec.reflections.vfs.ZipDir}
     * over a jar url (contains ".jar!/" in it's name), using Java's
     * {@link JarURLConnection}
     * 
     * <p>directory - creates a
     * {@link com.phyzicsz.parsec.reflections.vfs.SystemDir} over a file system
     * directory
     * 
     * <p>bundle - for bundle protocol, using eclipse FileLocator (should be
     * provided in classpath)
     * 
     * <p>jarInputStream - creates a {@link JarInputDir} over jar files, using
     * Java's JarInputStream
     */
    public enum DefaultUrlTypes implements UrlType {
        jarFile {
            @Override
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && hasJarFileInPath(url);
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },
        jarUrl {
            @Override
            public boolean matches(URL url) {
                return "jar".equals(url.getProtocol()) 
                        || "zip".equals(url.getProtocol()) 
                        || "wsjar".equals(url.getProtocol());
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        urlConnection.setUseCaches(false);
                        return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
                    }
                } catch (IOException ex) {
                    //fallback at this point
                }
                java.io.File file = getFile(url);
                if (file != null) {
                    return new ZipDir(new JarFile(file));
                }
                return null;
            }
        },
        directory {
            @Override
            public boolean matches(URL url) {
                if (url.getProtocol().equals("file") && !hasJarFileInPath(url)) {
                    java.io.File file = getFile(url);
                    return file != null && file.isDirectory();
                } else {
                    return false;
                }
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },
        bundle {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.getProtocol().startsWith("bundle");
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                return fromUrl((URL) ClasspathUtils.contextClassLoader()
                        .loadClass("org.eclipse.core.runtime.FileLocator")
                        .getMethod("resolve", URL.class).invoke(null, url));
            }
        },
        jarInputStream {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.toExternalForm().contains(".jar");
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new JarInputDir(url);
            }
        }
    }
}
