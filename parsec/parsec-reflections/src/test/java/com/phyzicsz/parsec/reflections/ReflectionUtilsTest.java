package com.phyzicsz.parsec.reflections;

import static com.phyzicsz.parsec.reflections.ReflectionUtils.*;
import static com.phyzicsz.parsec.reflections.ReflectionsTest.are;
import com.phyzicsz.parsec.reflections.scanners.FieldAnnotationsScanner;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author mamo
 */
@SuppressWarnings("unchecked")
public class ReflectionUtilsTest {

    @Test
    public void getAllTest() {
        assertThat(getAllSuperTypes(TestModel.C3.class, withAnnotation(TestModel.AI1.class)), are(TestModel.I1.class));

        Set<Method> allMethods = getAllMethods(TestModel.C4.class, withModifier(Modifier.PUBLIC), withReturnType(void.class));
        Set<Method> allMethods1 = getAllMethods(TestModel.C4.class, withPattern("public.*.void .*"));

        assertTrue(allMethods.containsAll(allMethods1) && allMethods1.containsAll(allMethods));
        assertThat(allMethods1, names("m1"));

        assertThat(getAllMethods(TestModel.C4.class, withAnyParameterAnnotation(TestModel.AM1.class)), names("m4"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class)), names("f1", "f2"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
            public String value() {return "2";}
            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}})),
                names("f2"));

        assertThat(getAllFields(TestModel.C4.class, withTypeAssignableTo(String.class)), names("f1", "f2", "f3"));

        assertThat(getAllConstructors(TestModel.C4.class, withParametersCount(0)), names(TestModel.C4.class.getName()));

        List<String> subTypes = toStringList(getAllAnnotations(TestModel.C3.class));
        Assertions.assertThat(subTypes)
//                .contains("class another.project.AnotherTestModel$C2")
                .contains("@java.lang.annotation.Documented()")
                .contains("@java.lang.annotation.Inherited()")
                .contains("@java.lang.annotation.Retention(RUNTIME)")
                .contains("@java.lang.annotation.Target(ANNOTATION_TYPE)")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$AC1()")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$AC1n()")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$AC2(ugh?!)")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$AI1()")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$AI2()")
                .contains("@com.phyzicsz.parsec.reflections.TestModel$MAI1()");
                
        
//        assertEquals(toStringSorted(getAllAnnotations(TestModel.C3.class)),
//                "[@java.lang.annotation.Documented(), " +
//                        "@java.lang.annotation.Inherited(), " +
//                        "@java.lang.annotation.Retention(value=RUNTIME), " +
//                        "@java.lang.annotation.Target(value=ANNOTATION_TYPE), " +
//                        "@org.reflections.TestModel$AC1(), " +
//                        "@org.reflections.TestModel$AC1n(), " +
//                        "@org.reflections.TestModel$AC2(value=ugh?!), " +
//                        "@org.reflections.TestModel$AI1(), " +
//                        "@org.reflections.TestModel$AI2(), " +
//                        "@org.reflections.TestModel$MAI1()]");

        Method m4 = getMethods(TestModel.C4.class, withName("m4")).iterator().next();
        assertEquals(m4.getName(), "m4");
        assertTrue(getAnnotations(m4).isEmpty());
    }

    @Test public void withParameter() throws Exception {
        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test
    public void withParametersAssignableFromTest() throws Exception {
        //Check for null safe
        getAllMethods(Collections.class, withModifier(Modifier.STATIC), withParametersAssignableFrom());

        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableFrom(Iterable.class), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test public void withReturn() {
        Set<Method> returnMember = getAllMethods(Class.class, withReturnTypeAssignableTo(Member.class));
        Set<Method> returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Method.class));

        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));

        returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Field.class));
        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));
    }

    @Test
    public void getAllAndReflections() {
        Reflections reflections = new Reflections(TestModel.class, new FieldAnnotationsScanner());

        Set<Field> af1 = reflections.getFieldsAnnotatedWith(TestModel.AF1.class);
        Set<? extends Field> allFields = ReflectionUtils.getAll(af1, withModifier(Modifier.PROTECTED));
        assertEquals(1, allFields.size());
        assertThat(allFields, names("f2"));
    }

    private Set<String> names(Set<? extends Member> o) {
        return o.stream().map(Member::getName).collect(Collectors.toSet());
    }

    private BaseMatcher<Set<? extends Member>> names(final String... namesArray) {
        return new BaseMatcher<Set<? extends Member>>() {

                public boolean matches(Object o) {
                    Collection<String> transform = names((Set<Member>) o);
                    final Collection<?> names = Arrays.asList(namesArray);
                    return transform.containsAll(names) && names.containsAll(transform);
                }

                public void describeTo(Description description) {
                }
            };
    }

    public static String toStringSorted(Set<?> set) {
        return set.stream()
                .map(o -> o.toString().replace("[", "").replace("]", "").replace("{", "").replace("}", "").replace("\"", ""))
                .sorted().collect(Collectors.toList()).toString();
    }
    
    public static List<String> toStringList(Set<?> set) {
        return set.stream()
                .map(o -> o.toString().replace("[", "").replace("]", "").replace("{", "").replace("}", "").replace("\"", ""))
                .sorted().collect(Collectors.toList());
    }
}
