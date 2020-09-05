package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.MoreTestsModel.CyclicAnnotation;
import com.phyzicsz.parsec.reflections.MoreTestsModel.Meta;
import com.phyzicsz.parsec.reflections.MoreTestsModel.MultiName;
import com.phyzicsz.parsec.reflections.MoreTestsModel.Name;
import com.phyzicsz.parsec.reflections.MoreTestsModel.Names;
import com.phyzicsz.parsec.reflections.MoreTestsModel.ParamNames;
import com.phyzicsz.parsec.reflections.MoreTestsModel.SingleName;
import static com.phyzicsz.parsec.reflections.ReflectionUtilsTest.toStringList;
import static com.phyzicsz.parsec.reflections.ReflectionsTest.are;
import com.phyzicsz.parsec.reflections.scanners.MethodParameterNamesScanner;
import com.phyzicsz.parsec.reflections.scanners.ResourcesScanner;
import com.phyzicsz.parsec.reflections.scanners.SubTypesScanner;
import com.phyzicsz.parsec.reflections.scanners.TypeAnnotationsScanner;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.ConfigurationBuilder;
import com.phyzicsz.parsec.reflections.util.FilterBuilder;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MoreTests {

    @Test
    public void test_cyclic_annotation() {
        Reflections reflections = new Reflections(MoreTestsModel.class);
        assertThat(reflections.getTypesAnnotatedWith(CyclicAnnotation.class),
                are(CyclicAnnotation.class));
    }

    @Test
    public void no_exception_when_configured_scanner_store_is_empty() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix")));

        reflections.getSubTypesOf(String.class);
    }

    @Test
    public void getAllAnnotated_returns_meta_annotations() {
        Reflections reflections = new Reflections(MoreTestsModel.class);
        for (Class<?> type: reflections.getTypesAnnotatedWith(Meta.class)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }

        Meta meta = new Meta() {
            @Override public String value() { return "a"; }
            @Override public Class<? extends Annotation> annotationType() { return Meta.class; }
        };
        for (Class<?> type: reflections.getTypesAnnotatedWith(meta)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }
    }

//    @Test
//    public void test_java_9_subtypes_of_Object() {
//        Reflections reflections = new Reflections(new ConfigurationBuilder()
//                .setUrls(ClasspathHelper.forClass(Object.class))
//                .setScanners(new SubTypesScanner(false)));
//        Set<?> components = reflections.getSubTypesOf(Object.class);
//        
//        //should this really be empty?
//        assertTrue(components.isEmpty());
//    }

    @Test
    public void test_custom_url_class_loader() throws MalformedURLException {
        URL externalUrl = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
        URLClassLoader externalClassLoader = new URLClassLoader(new URL[]{externalUrl}, Thread.currentThread().getContextClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forClass(TestModel.class))
                .addUrls(externalUrl)
                .addClassLoaders(externalClassLoader));

        List<String> subTypes = toStringList(reflections.getSubTypesOf(TestModel.C1.class));
        Assertions.assertThat(subTypes)
//                .contains("class another.project.AnotherTestModel$C2")
                .contains("class com.phyzicsz.parsec.reflections.TestModel$C2")
                .contains("class com.phyzicsz.parsec.reflections.TestModel$C3")
                .contains("class com.phyzicsz.parsec.reflections.TestModel$C5");
                
        
//        assertEquals(toStringSorted(reflections.getSubTypesOf(TestModel.C1.class)),
//                "[class another.project.AnotherTestModel$C2, " +
//                        "class com.phyzicsz.parsec.reflections.TestModel$C2, " +
//                        "class com.phyzicsz.parsec.reflections.TestModel$C3, " +
//                        "class com.phyzicsz.parsec.reflections.TestModel$C5]");
    }

    @Test
    public void resources_scanner_filters_classes() {
        Reflections reflections = new Reflections(new ResourcesScanner());
        Set<String> keys = reflections.getStore().keys(ResourcesScanner.class.getSimpleName());
        assertTrue(keys.stream().noneMatch(res -> res.endsWith(".class")));
    }

    @Test
    public void test_repeatable() {
        Reflections ref = new Reflections(MoreTestsModel.class);
        Set<Class<?>> clazzes = ref.getTypesAnnotatedWith(Name.class);
        assertTrue(clazzes.contains(SingleName.class));
        assertFalse(clazzes.contains(MultiName.class));

        clazzes = ref.getTypesAnnotatedWith(Names.class);
        assertFalse(clazzes.contains(SingleName.class));
        assertTrue(clazzes.contains(MultiName.class));
    }

    @Test
    public void test_method_param_names_not_local_vars() throws NoSuchMethodException {
        Reflections reflections = new Reflections(MoreTestsModel.class, new MethodParameterNamesScanner());

        Class<ParamNames> clazz = ParamNames.class;
        assertEquals(reflections.getConstructorParamNames(clazz.getConstructor(String.class)).toString(),
                "[param1]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test", String.class, String.class)).toString(),
                "[testParam1, testParam2]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test", String.class)).toString(),
                "[testParam]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test2", String.class)).toString(),
                "[testParam]");

    }
}
