package com.phyzicsz.parsec.reflections.filter;

import com.google.common.base.Splitter;
import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import com.phyzicsz.parsec.reflections.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Builds include/exclude filters for Reflections.
 *
 * <p>For example:
 * <pre>{@code
 * Predicate<String> filter1 = FilterBuilder.parsePackages("-java, "-javax");
 * Predicate<String> filter2 = new FilterBuilder().include(".*").exclude("java.*");
 * }
 * </pre>
 */
public class FilterBuilder implements Predicate<String> {

    private final List<Predicate<String>> chain;

    public FilterBuilder() {
        chain = new ArrayList<>();
    }

    private FilterBuilder(final Collection<Predicate<String>> filters) {
        chain = new ArrayList<>(filters);
    }

    /**
     * Include based on a regular expression.
     *
     * @param regex the regex pattern
     * @return fluent builder
     */
    public FilterBuilder include(final String regex) {
        return add(new Include(regex));
    }

    /**
     * Exclude based on a regular expression.
     *
     * @param regex the regex pattern
     * @return fluent builder
     */
    public FilterBuilder exclude(final String regex) {
        add(new Exclude(regex));
        return this;
    }

    /**
     * Add a Predicate to the chain of predicates.
     *
     * @param filter the predicate filter
     * @return fluent builder
     */
    public FilterBuilder add(Predicate<String> filter) {
        chain.add(filter);
        return this;
    }

    /**
     * Include a package of a given class.
     *
     * @param classz the class to include
     * @return fluent builder
     */
    public FilterBuilder includePackage(final Class<?> classz) {
        return add(new Include(packageNameRegex(classz)));
    }
    
    /**
     * Include packages of given prefixes.
     *
     * @param prefixes the prefixes to include
     * @return fluent builder
     */
    public FilterBuilder includePackage(final String... prefixes) {
        for (String prefix : prefixes) {
            add(new Include(prefix(prefix)));
        }
        return this;
    }

    /**
     * Exclude a package of a given class.
     *
     * @param classz the class to exclude
     * @return fluent builder
     */
    public FilterBuilder excludePackage(final Class<?> classz) {
        return add(new Exclude(packageNameRegex(classz)));
    }

    /**
     * Exclude packages of a given prefix.
     *
     * @param prefixes the prefixes to exclude
     * @return fluent builder
     */
    public FilterBuilder excludePackage(final String... prefixes) {
        for (String prefix : prefixes) {
            add(new Exclude(prefix(prefix)));
        }
        return this;
    }

    private static String packageNameRegex(Class<?> classz) {
        return prefix(classz.getPackage().getName() + ".");
    }

    public static String prefix(String qualifiedName) {
        return qualifiedName.replace(".", "\\.") + ".*";
    }

    @Override
    public String toString() {
        return Utils.join(chain, ", ");
    }

    @Override
    public boolean test(String regex) {
        boolean accept = chain.isEmpty() || chain.get(0) instanceof Exclude;

        for (Predicate<String> filter : chain) {
            if (accept && filter instanceof Include) {
                continue;
            } //skip if this filter won't change
            if (!accept && filter instanceof Exclude) {
                continue;
            }
            accept = filter.test(regex);
            if (!accept && filter instanceof Exclude) {
                break;
            } //break on first exclusion
        }
        return accept;
    }

    /**
     * Parses a string representation of an include/exclude filter.
     *
     * <p>The given includeExcludeString is a comma separated list of regexes, each
     * starting with either + or - to indicate include/exclude.
     * 
     * <p>For example parsePackages("-java\\..*, -javax\\..*, -sun\\..*,
     * -com\\.sun\\..*") or parse("+com\\.myn\\..*,-com\\.myn\\.excluded\\..*").
     * Note that "-java\\..*" will block "java.foo" but not "javax.foo".
     * 
     * <p>See also the more useful {@link FilterBuilder#parsePackages(String)}
     * method.
     *
     * @param includeExcludeString the string to include/exclude based on
     * @return fluent builder
     */
    public static FilterBuilder parse(String includeExcludeString) {
        List<Predicate<String>> filters = new ArrayList<>();

        if (!includeExcludeString.isEmpty()) {
            for (String string : Splitter.on(',').split(includeExcludeString)) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);

                Predicate<String> filter;
                switch (prefix) {
                    case '+':
                        filter = new Include(pattern);
                        break;
                    case '-':
                        filter = new Exclude(pattern);
                        break;
                    default:
                        throw new ReflectionsException("includeExclude should start with either + or -");
                }

                filters.add(filter);
            }

            return new FilterBuilder(filters);
        } else {
            return new FilterBuilder();
        }
    }

    /**
     * Parses a string representation of an include/exclude filter.
     *
     * <p>The given includeExcludeString is a comma separated list of package name
     * segments, each starting with either + or - to indicate include/exclude.
     * 
     * <p>For example parsePackages("-java, -javax, -sun, -com.sun") or
     * parse("+com.myn,-com.myn.excluded"). Note that "-java" will block
     * "java.foo" but not "javax.foo".
     * 
     * <p>The input strings "-java" and "-java." are equivalent.
     *
     * @param includeExcludeString the string to include/exclude based on
     * @return fluent builder
     */
    public static FilterBuilder parsePackages(String includeExcludeString) {
        List<Predicate<String>> filters = new ArrayList<>();

        if (!includeExcludeString.isEmpty()) {
            for (String string : Splitter.on(',').split(includeExcludeString)) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);
                if (!pattern.endsWith(".")) {
                    pattern += ".";
                }
                pattern = prefix(pattern);

                Predicate<String> filter;
                switch (prefix) {
                    case '+':
                        filter = new Include(pattern);
                        break;
                    case '-':
                        filter = new Exclude(pattern);
                        break;
                    default:
                        throw new ReflectionsException("includeExclude should start with either + or -");
                }

                filters.add(filter);
            }

            return new FilterBuilder(filters);
        } else {
            return new FilterBuilder();
        }
    }
}
