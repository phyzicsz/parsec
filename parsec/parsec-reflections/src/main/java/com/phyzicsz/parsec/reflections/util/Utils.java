package com.phyzicsz.parsec.reflections.util;


import com.phyzicsz.parsec.reflections.ReflectionUtils;
import com.phyzicsz.parsec.reflections.ReflectionsException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of convenience methods for Reflection.
 * 
 */
public abstract class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String repeat(String string, int times) {
        return IntStream.range(0, times).mapToObj(i -> string).collect(Collectors.joining());
    }

    /**
     * Prepares a file (path existance check etc).
     * 
     * @param filename the file name to validate
     * @return a File handle
     */
    public static File prepareFile(String filename) {
        File file = new File(filename);
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        return file;
    }

    /**
     * Get a member from a description.
     * 
     * @param descriptor the description to match on
     * @param classLoaders the class loader
     * @return the member 
     * @throws ReflectionsException throws exception on error
     */
    public static Member getMemberFromDescriptor(String descriptor, ClassLoader... classLoaders) 
            throws ReflectionsException {
        
        int p0 = descriptor.lastIndexOf('(');
        String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
        String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(')')) : "";

        int p1 = Math.max(memberKey.lastIndexOf('.'), memberKey.lastIndexOf("$"));
        String className = memberKey.substring(0, p1);
        String memberName = memberKey.substring(p1 + 1);

        Class<?>[] parameterTypes = null;
        if (!methodParameters.isEmpty()) {
            String[] parameterNames = methodParameters.split(",");
            parameterTypes = Arrays.stream(parameterNames).map(name -> 
                    ReflectionUtils.forName(name.trim(), classLoaders))
                        .toArray(Class<?>[]::new);
        }

        Class<?> classz = ReflectionUtils.forName(className, classLoaders);
        while (classz != null) {
            try {
                if (!descriptor.contains("(")) {
                    return classz.isInterface() ? classz.getField(memberName) : 
                            classz.getDeclaredField(memberName);
                } else if (isConstructor(descriptor)) {
                    return classz.isInterface() ? classz.getConstructor(parameterTypes) : 
                            classz.getDeclaredConstructor(parameterTypes);
                } else {
                    return classz.isInterface() ? classz.getMethod(memberName, parameterTypes) : 
                            classz.getDeclaredMethod(memberName, parameterTypes);
                }
            } catch (NoSuchFieldException | NoSuchMethodException | SecurityException e) {
                classz = classz.getSuperclass();
            }
        }
        throw new ReflectionsException("Can't resolve member named " + memberName + " for class " + className);
    }

    /**
     * Gets Methods from a description.
     * 
     * @param annotatedWith dscription to match
     * @param classLoaders class loaders
     * @return the matching members
     */
    public static Set<Method> getMethodsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Method> result = new HashSet<>();
        for (String annotated : annotatedWith) {
            if (!isConstructor(annotated)) {
                Method member = (Method) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    /**
     * Gets Constructors from a description.
     * 
     * @param annotatedWith matching descriptor
     * @param classLoaders class loader
     * @return the matching constructors
     */
    public static Set<Constructor<?>> getConstructorsFromDescriptors(Iterable<String> annotatedWith, 
            ClassLoader... classLoaders) {
        Set<Constructor<?>> result = new HashSet<>();
        for (String annotated : annotatedWith) {
            if (isConstructor(annotated)) {
                Constructor<?> member = (Constructor<?>) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    /**
     * Gets Members from a description.
     * 
     * @param values the matching values
     * @param classLoaders class loaders
     * @return the matching Members
     */
    public static Set<Member> getMembersFromDescriptors(Iterable<String> values, ClassLoader... classLoaders) {
        Set<Member> result = new HashSet<>();
        for (String value : values) {
            try {
                result.add(Utils.getMemberFromDescriptor(value, classLoaders));
            } catch (ReflectionsException e) {
                throw new ReflectionsException("Can't resolve member named " + value, e);
            }
        }
        return result;
    }

    /**
     * Gets field from a string.
     * 
     * @param field the field to match
     * @param classLoaders class loader
     * @return the matching Field
     */
    public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return ReflectionUtils.forName(className, classLoaders).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        }
    }

    /**
     * Closes an Input stream.
     * 
     * @param closeable the closeable stream
     */
    public static void close(InputStream closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.warn("Could not close InputStream", e);
        }
    }

    /**
     * Tests if function is a constructor.
     * 
     * @param fqn the function
     * @return boolean true if function is a constructor
     */
    public static boolean isConstructor(String fqn) {
        return fqn.contains("init>");
    }

   
    /**
     * Returns the name of the attribute.
     * 
     * @param type the type to get the name from
     * @return the name of the attribute
     */
    public static String name(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        } else {
            int dim = 0;
            while (type.isArray()) {
                dim++;
                type = type.getComponentType();
            }
            return type.getName() + repeat("[]", dim);
        }
    }

    /**
     * Gets the name of a constructor.
     * 
     * @param constructor the matching constructor
     * @return the name
     */
    public static String name(Constructor<?> constructor) {
        return new StringBuilder()
                .append(constructor.getName())
                .append(".")
                .append("<init>")
                .append("(")
                .append(join(names(constructor.getParameterTypes()), ", "))
                .append(")")
                .toString();            
    }

    /**
     * Gets the name of a method.
     * 
     * @param method the matching method
     * @return the name of the method
     */
    public static String name(Method method) {
        return new StringBuilder()
                .append(method.getDeclaringClass().getName())
                .append(".")
                .append(method.getName())
                .append("(")
                .append(join(names(method.getParameterTypes()), ", "))
                .append(")")
                .toString();
    }
    
    /** 
     * Gets the name of a field.
     * 
     * @param field the matching field
     * @return the name of the field
     */
    public static String name(Field field) {
        return new StringBuilder()
                .append(field.getDeclaringClass().getName())
                .append(".")
                .append(field.getName())
                .toString();
    }
    
    public static List<String> names(Collection<Class<?>> types) {
        return types.stream().map(Utils::name).collect(Collectors.toList());
    }

    public static List<String> names(Class<?>... types) {
        return names(Arrays.asList(types));
    }

    public static String index(Class<?> scannerClass) {
        return scannerClass.getSimpleName();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Predicate<T> and(Predicate... predicates) {
        return Arrays.stream(predicates).reduce(t -> true, Predicate::and);
    }

    public static String join(Collection<?> elements, String delimiter) {
        return elements.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }

    @SafeVarargs
    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T>... predicates) {
        return result.stream().filter(and(predicates)).collect(Collectors.toSet());
    }

    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T> predicate) {
        return result.stream().filter(predicate).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <T> Set<T> filter(T[] result, Predicate<? super T>... predicates) {
        return Arrays.stream(result).filter(and(predicates)).collect(Collectors.toSet());
    }
}
