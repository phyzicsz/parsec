package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenient java reflection helper methods.
 * 
 * <p>1. some helper methods to get type by name:
 * {@link #forName(String, ClassLoader...)} and
 * {@link #forNames(Collection, ClassLoader...)} )}
 * 
 * <p>2. some helper methods to get all
 * types/methods/fields/constructors/properties matching some predicates,
 * generally:
 * <pre>{@code Set&#60?> result = getAllXXX(type/s, withYYY) }</pre>
 * 
 * <p>where get methods are:
 * <ul>
 * <li>{@link #getAllSuperTypes(Class, java.util.function.Predicate...)}
 * <li>{@link #getAllFields(Class, java.util.function.Predicate...)}
 * <li>{@link #getAllMethods(Class, java.util.function.Predicate...)}
 * <li>{@link #getAllConstructors(Class, java.util.function.Predicate...)}
 * </ul>
 * 
 * <p>and predicates included here all starts with "with", such as
 * <ul>
 * <li>{@link #withAnnotation(java.lang.annotation.Annotation)}
 * <li>{@link #withModifier(int)}
 * <li>{@link #withName(String)}
 * <li>{@link #withParameters(Class[])}
 * <li>{@link #withAnyParameterAnnotation(Class)}
 * <li>{@link #withParametersAssignableTo(Class[])}
 * <li>{@link #withParametersAssignableFrom(Class[])}
 * <li>{@link #withPrefix(String)}
 * <li>{@link #withReturnType(Class)}
 * <li>{@link #withType(Class)}
 * <li>{@link #withTypeAssignableTo}
 * </ul>
 *
 * <p><br>
 * for example, getting all getters would be:
 * <pre>{@code
 *      Set&#60Method> getters = getAllMethods(someClasses,
 *              Predicates.and(
 *                      withModifier(Modifier.PUBLIC),
 *                      withPrefix("get"),
 *                      withParametersCount(0)));
 * }
 * </pre>
 *
 */
@SuppressWarnings("unchecked")
public abstract class ReflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    /**
     * would include {@code Object.class} when
     * {@link #getAllSuperTypes(Class, java.util.function.Predicate[])}. default
     * is false.
     */
    public static boolean includeObject = false;

    /**
     * Get all super types of given {@code type}, including, optionally filtered
     * by {@code predicates}.
     * 
     * <p>include {@code Object.class} if {@link #includeObject} is true
     *
     * @param type type information
     * @param predicates the predicates to match
     * @return the super types for the matching predicates
     */
    public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
        Set<Class<?>> result = new LinkedHashSet<>();
        if (type != null && (includeObject || !type.equals(Object.class))) {
            result.add(type);
            for (Class<?> supertype : getSuperTypes(type)) {
                result.addAll(getAllSuperTypes(supertype));
            }
        }
        return Utils.filter(result, predicates);
    }

    /**
     * Get the immediate supertype and interfaces of the given {@code type}.
     *
     * @param type type information
     * @return the supertypes for the class
     */
    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        if (superclass != null && (includeObject || !superclass.equals(Object.class))) {
            result.add(superclass);
        }
        if (interfaces != null && interfaces.length > 0) {
            result.addAll(Arrays.asList(interfaces));
        }
        return result;
    }

    /**
     * Get all methods of given {@code type}, up the super class hierarchy,
     * optionally filtered by {@code predicates}.
     *
     * @param type type information
     * @param predicates predicates to match
     * @return the methods for the matching predicates
     */
    public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method>... predicates) {
        Set<Method> result = new HashSet<>();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getMethods(t, predicates));
        }
        return result;
    }

    /**
     * Get methods of given {@code type}, optionally filtered by
     * {@code predicates}.
     *
     * @param t type information
     * @param predicates predicates to match
     * @return the methods for the matching predictes
     */
    public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
        return Utils.filter(t.isInterface() ? t.getMethods() : t.getDeclaredMethods(), predicates);
    }

    /**
     * Get all constructors of given {@code type}, up the super class hierarchy,
     * optionally filtered by {@code predicates}.
     *
     * @param type type information
     * @param predicates the matching predicates
     * @return the constuctors for the matching predicates
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Set<Constructor> getAllConstructors(final Class<?> type, 
            Predicate<? super Constructor>... predicates) {
        Set<Constructor> result = new HashSet<>();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getConstructors(t, predicates));
        }
        return result;
    }

    /**
     * Get constructors of given {@code type}, optionally filtered by
     * {@code predicates}.
     *
     * @param t type information
     * @param predicates the matching predicates
     * @return the constuctors for the matching predicates
     */
    public static Set<Constructor<?>> getConstructors(Class<?> t, Predicate<? super Constructor<?>>... predicates) {
        return Utils.filter(t.getDeclaredConstructors(), predicates);
    }

    /**
     * Get all fields of given {@code type}, up the super class hierarchy,
     * optionally filtered by {@code predicates}.
     *
     * @param type the type information
     * @param predicates the matching predicates
     * @return the fields for the matching predicates
     */
    public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
        Set<Field> result = new HashSet<>();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getFields(t, predicates));
        }
        return result;
    }

    /**
     * Get fields of given {@code type}, optionally filtered by
     * {@code predicates}.
     *
     * @param type the type information
     * @param predicates the matching predicates
     * @return the fields for the matching predicates
     */
    public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
        return Utils.filter(type.getDeclaredFields(), predicates);
    }

    /**
     * Get all annotations of given {@code type}, up the super class hierarchy,
     * optionally filtered by {@code predicates}.
     *
     * @param <T> the class information
     * @param type type information
     * @param predicates the matching predicates
     * @return the annotations for the matching predicates
     */
    public static <T extends AnnotatedElement> Set<Annotation> getAllAnnotations(T type, 
            Predicate<Annotation>... predicates) {
        Set<Annotation> result = new LinkedHashSet<>();
        List<AnnotatedElement> keys = new ArrayList<>();
        if (type instanceof Class) {
            keys.addAll(getAllSuperTypes((Class<?>) type));
        }
        for (int i = 0; i < keys.size(); i++) {
            for (Annotation annotation : getAnnotations(keys.get(i), predicates)) {
                if (result.add(annotation)) {
                    keys.add(annotation.annotationType());
                }
            }
        }
        return result;
    }

    /**
     * Get annotations of given {@code type}, optionally honorInherited,
     * optionally filtered by {@code predicates}.
     *
     * @param <T> class information
     * @param type type information
     * @param predicates the matching predicates
     * @return the annotations for the matching predicates
     */
    public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, 
            Predicate<Annotation>... predicates) {
        return Utils.filter(type.getDeclaredAnnotations(), predicates);
    }

    /**
     * Filter all given {@code elements} with {@code predicates}.
     *
     * @param <T> class information
     * @param elements elements to filter
     * @param predicates matching predicates
     * @return all elements matching predicates
     */
    public static <T extends AnnotatedElement> Set<T> getAll(final Set<T> elements, 
            Predicate<? super T>... predicates) {
        return Utils.filter(elements, predicates);
    }

    /**
     * Find member name equals given {@code name}.
     *
     * @param <T> the class information
     * @param name the member name
     * @return the member with name
     */
    public static <T extends Member> Predicate<T> withName(final String name) {
        return input -> input != null && input.getName().equals(name);
    }

    /**
     * Where member name startsWith given {@code prefix}.
     *
     * @param <T> class information
     * @param prefix the prefix for the member name
     * @return the member with the given prefix
     */
    public static <T extends Member> Predicate<T> withPrefix(final String prefix) {
        return input -> input != null && input.getName().startsWith(prefix);
    }

    /**
     * Find member's {@code toString} matching a given {@code regex}.
     * 
     * <p>for example:
     * <pre>
     *  getAllMethods(someClass, withPattern("public void .*"))
     * </pre>
     *
     * @param <T> class information
     * @param regex the regex to match
     * @return the member matched from regex
     */
    public static <T extends AnnotatedElement> Predicate<T> withPattern(final String regex) {
        return input -> Pattern.matches(regex, input.toString());
    }

    /**
     * FInd element annotated with given {@code annotation}.
     *
     * @param <T> class information
     * @param annotation the annotation
     * @return find element annotated with matching annotation
     */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(
            final Class<? extends Annotation> annotation) {
        return input -> input != null && input.isAnnotationPresent(annotation);
    }

    /**
     * Find element annotated with given {@code annotation}, including
     * member matching.
     *
     * @param <T> class information
     * @param annotation the annotation to match
     * @return the element annotated with matching annotation
     */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
        return input -> input != null && input.isAnnotationPresent(annotation.annotationType())
                && areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
    }
    
    /**
     * Find element annotated with given {@code annotations}.
     *
     * @param <T> class information
     * @param annotations the annotations to match
     * @return the elements with annotated with matching annotation
     */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotations(
            final Class<? extends Annotation>... annotations) {
        return input -> input != null && Arrays.equals(annotations, annotationTypes(input.getAnnotations()));
    }

    /**
     * Find element annotated with given {@code annotations}, including
     * member matching.
     *
     * @param <T> class information
     * @param annotations the annotations to match
     * @return the elements with matching annotations
     */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Annotation... annotations) {
        return input -> {
            if (input != null) {
                Annotation[] inputAnnotations = input.getAnnotations();
                if (inputAnnotations.length == annotations.length) {
                    return IntStream.range(0, inputAnnotations.length)
                            .allMatch(i -> areAnnotationMembersMatching(inputAnnotations[i], annotations[i]));
                }
            }
            return true;
        };
    }

    /**
     * Find method/constructor parameter types equals given {@code types}.
     *
     * @param types type information
     * @return the method/constructor with matching parameter types
     */
    public static Predicate<Member> withParameters(final Class<?>... types) {
        return input -> Arrays.equals(parameterTypes(input), types);
    }

    /**
     * Find member parameter types assignable to given {@code types}.
     *
     * @param types the type information
     * @return the members matching the assignable type
     */
    public static Predicate<Member> withParametersAssignableTo(final Class<?>... types) {
        return input -> isAssignable(types, parameterTypes(input));
    }

    /**
     * Find method/constructor parameter types assignable from given
     * {@code types}.
     *
     * @param types type information
     * @return the method/constuctor matching the assignable types
     */
    public static Predicate<Member> withParametersAssignableFrom(final Class<?>... types) {
        return input -> isAssignable(parameterTypes(input), types);
    }

    /**
     * Find method/constructor parameters count equal given {@code count}.
     *
     * @param count the parameter count
     * @return the method/constructors with matching parameter count.
     */
    public static Predicate<Member> withParametersCount(final int count) {
        return input -> input != null && parameterTypes(input).length == count;
    }

    /**
     * Find method/constructor has any parameter with an annotation matches
     * given {@code annotations}.
     *
     * @param annotationClass the annotation class to match
     * @return the method/constuctors with matching annotations
     */
    public static Predicate<Member> withAnyParameterAnnotation(final Class<? extends Annotation> annotationClass) {
        return input -> input != null 
                && annotationTypes(parameterAnnotations(input))
                        .stream()
                        .anyMatch(input1 -> input1.equals(annotationClass));  
    }

    /**
     * Find method/constructor has any parameter with an annotation matches
     * given {@code annotation}, including member matching.
     *
     * @param annotation the matching annotation
     * @return the method/constuctor with matching annotation
     */
    public static Predicate<Member> withAnyParameterAnnotation(final Annotation annotation) {
        return input -> input != null && parameterAnnotations(input)
                .stream()
                .anyMatch(input1 -> areAnnotationMembersMatching(annotation, input1));
    }

    /**
     * Find field type equal given {@code type}.
     *
     * @param <T> class information
     * @param type type information
     * @return the field with matching types
     */
    public static <T> Predicate<Field> withType(final Class<T> type) {
        return input -> input != null && input.getType().equals(type);
    }

    /**
     * Find field type assignable to given {@code type}.
     *
     * @param <T> class information
     * @param type type information
     * @return the field with matching type
     */
    public static <T> Predicate<Field> withTypeAssignableTo(final Class<T> type) {
        return input -> input != null && type.isAssignableFrom(input.getType());
    }

    /**
     * Find method return type equal given {@code type}.
     *
     * @param <T> class information
     * @param type type information
     * @return the method with matching type
     */
    public static <T> Predicate<Method> withReturnType(final Class<T> type) {
        return input -> input != null && input.getReturnType().equals(type);
    }

    /**
     * Find method return type assignable from given {@code type}.
     *
     * @param <T> class information
     * @param type type information
     * @return the method with matching type
     */
    public static <T> Predicate<Method> withReturnTypeAssignableTo(final Class<T> type) {
        return input -> input != null && type.isAssignableFrom(input.getReturnType());
    }

    /**
     * Find member modifier matches given {@code mod}.
     * 
     * <p>for example:
     * <pre>
     * withModifier(Modifier.PUBLIC)
     * </pre>
     *
     * @param <T> class information
     * @param mod the modifier
     * @return member with matching modifier
     */
    public static <T extends Member> Predicate<T> withModifier(final int mod) {
        return input -> input != null && (input.getModifiers() & mod) != 0;
    }

    /**
     * Find class modifier matches given {@code mod}.
     * 
     * <p>for example:
     * <pre>
     * withModifier(Modifier.PUBLIC)
     * </pre>
     *
     * @param mod modifier
     * @return the class with matching modifier
     */
    public static Predicate<Class<?>> withClassModifier(final int mod) {
        return input -> input != null && (input.getModifiers() & mod) != 0;
    }

    /**
     * Resolve a java type name to a Class.
     * 
     * <p>if optional {@link ClassLoader}s are not specified, then both
     * {@link org.reflections.util.ClasspathHelper#contextClassLoader()} and
     * {@link org.reflections.util.ClasspathHelper#staticClassLoader()} are used
     *
     * @param typeName type name
     * @param classLoaders class loader
     * @return resolved java type to class name
     */
    public static Class<?> forName(String typeName, ClassLoader... classLoaders) {
        if (getPrimitiveNames().contains(typeName)) {
            return getPrimitiveTypes().get(getPrimitiveNames().indexOf(typeName));
        } else {
            String type;
            if (typeName.contains("[")) {
                int i = typeName.indexOf("[");
                type = typeName.substring(0, i);
                String array = typeName.substring(i).replace("]", "");

                if (getPrimitiveNames().contains(type)) {
                    type = getPrimitiveDescriptors().get(getPrimitiveNames().indexOf(type));
                } else {
                    type = "L" + type + ";";
                }

                type = array + type;
            } else {
                type = typeName;
            }

            List<ReflectionsException> reflectionsExceptions = new ArrayList<>();
            for (ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)) {
                if (type.contains("[")) {
                    try {
                        return Class.forName(type, false, classLoader);
                    } catch (ClassNotFoundException e) {
                        reflectionsExceptions.add(
                                new ReflectionsException("could not get type for name " + typeName, e));
                    }
                }
                try {
                    return classLoader.loadClass(type);
                } catch (ClassNotFoundException e) {
                    reflectionsExceptions.add(
                            new ReflectionsException("could not get type for name " + typeName, e));
                }
            }

            if (logger.isTraceEnabled()) {
                for (ReflectionsException reflectionsException : reflectionsExceptions) {
                    logger.trace("could not get type for name " 
                            + typeName 
                            + " from any class loader", reflectionsException);
                }
            }

            return null;
        }
    }

    /**
     * Resolve all given string representation of types to a list of java
     * types.
     *
     * @param <T> class information
     * @param classes the classes to resolve
     * @param classLoaders class loader
     * @return the resolved names of the java types
     */
    public static <T> Set<Class<? extends T>> forNames(final Collection<String> classes, ClassLoader... classLoaders) {
        return classes.stream()
                .map(className -> (Class<? extends T>) forName(className, classLoaders))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Class<?>[] parameterTypes(Member member) {
        return member != null
                ? member.getClass() == Method.class ? ((Method) member).getParameterTypes()
                : member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
    }

    private static Set<Annotation> parameterAnnotations(Member member) {
        Annotation[][] annotations
                = member instanceof Method ? ((Method) member).getParameterAnnotations()
                        : member instanceof Constructor ? ((Constructor) member).getParameterAnnotations() : null;
        return Arrays.stream(annotations).flatMap(Arrays::stream).collect(Collectors.toSet());
    }

    private static Set<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
        return annotations.stream().map(Annotation::annotationType).collect(Collectors.toSet());
    }

    private static Class<? extends Annotation>[] annotationTypes(Annotation[] annotations) {
        return Arrays.stream(annotations).map(Annotation::annotationType).toArray(Class[]::new);
    }

    //
    private static List<String> primitiveNames;
    private static List<Class<?>> primitiveTypes;
    private static List<String> primitiveDescriptors;

    private static void initPrimitives() {
        if (primitiveNames == null) {
            primitiveNames = Arrays.asList(
                    "boolean", 
                    "char", 
                    "byte", 
                    "short", 
                    "int", 
                    "long", 
                    "float", 
                    "double", 
                    "void");
            primitiveTypes = Arrays.asList(
                    boolean.class, 
                    char.class, 
                    byte.class, 
                    short.class, 
                    int.class, 
                    long.class, 
                    float.class, 
                    double.class, 
                    void.class);
            primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
        }
    }

    private static List<String> getPrimitiveNames() {
        initPrimitives();
        return primitiveNames;
    }

    private static List<Class<?>> getPrimitiveTypes() {
        initPrimitives();
        return primitiveTypes;
    }

    private static List<String> getPrimitiveDescriptors() {
        initPrimitives();
        return primitiveDescriptors;
    }

    //
    private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
        if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
            for (Method method : annotation1.annotationType().getDeclaredMethods()) {
                try {
                    if (!method.invoke(annotation1).equals(method.invoke(annotation2))) {
                        return false;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new ReflectionsException(
                            String.format("could not invoke method %s on annotation %s", 
                                    method.getName(), 
                                    annotation1.annotationType()), e);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isAssignable(Class<?>[] childClasses, Class<?>[] parentClasses) {
        if (childClasses == null) {
            return parentClasses == null || parentClasses.length == 0;
        }
        if (childClasses.length != parentClasses.length) {
            return false;
        }
        return IntStream.range(0, childClasses.length)
                .noneMatch(i -> !parentClasses[i].isAssignableFrom(childClasses[i])
                || (parentClasses[i] == Object.class && childClasses[i] != Object.class));
    }
}
