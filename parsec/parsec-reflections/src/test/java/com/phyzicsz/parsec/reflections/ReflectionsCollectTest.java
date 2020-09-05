package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.Reflections;
import org.junit.BeforeClass;
import org.junit.Test;
import com.phyzicsz.parsec.reflections.scanners.MemberUsageScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodAnnotationsScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodParameterNamesScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodParameterScanner;
import com.phyzicsz.parsec.reflections.scanners.ResourcesScanner;
import com.phyzicsz.parsec.reflections.scanners.SubTypesScanner;
import com.phyzicsz.parsec.reflections.scanners.TypeAnnotationsScanner;
import com.phyzicsz.parsec.reflections.serializers.JsonSerializer;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.ConfigurationBuilder;
import com.phyzicsz.parsec.reflections.util.FilterBuilder;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.Assert.assertThat;
import static com.phyzicsz.parsec.reflections.util.Utils.index;

/** */
public class ReflectionsCollectTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        Reflections ref = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forClass(TestModel.class))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));

        ref.save(getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.xml");

        ref = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new MethodParameterScanner()));

        final JsonSerializer serializer = new JsonSerializer();
        ref.save(getUserDir() + "/target/test-classes" + "/META-INF/reflections/testModel-reflections.json", serializer);

        reflections = Reflections
                .collect()
                .merge(Reflections.collect("META-INF/reflections",
                        new FilterBuilder().include(".*-reflections.json"),
                        serializer));
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new FilterBuilder().include(".*\\.xml").include(".*\\.json");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new ResourcesScanner())
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Set<String> resources = reflections.getStore().keys(index(ResourcesScanner.class));
        assertThat(resources, are("resource1-reflections.xml", "resource2-reflections.xml",
                "testModel-reflections.xml", "testModel-reflections.json"));
    }
}
