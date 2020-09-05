package com.phyzicsz.parsec.reflections;

import com.phyzicsz.parsec.reflections.Reflections;
import org.junit.BeforeClass;
import org.junit.Test;
import com.phyzicsz.parsec.reflections.scanners.TypeElementsScanner;
import com.phyzicsz.parsec.reflections.serializers.JavaCodeSerializer;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.ConfigurationBuilder;
import com.phyzicsz.parsec.reflections.util.FilterBuilder;

import java.util.Collections;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static com.phyzicsz.parsec.reflections.TestModel.*;

/** */
public class JavaCodeSerializerTest {

    @BeforeClass
    public static void generateAndSave() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new TypeElementsScanner().includeFields().publicOnly(false))
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        //save
        String filename = ReflectionsTest.getUserDir() + "/src/test/java/org.reflections.MyTestModelStore";
        reflections.save(filename, new JavaCodeSerializer());
    }

    @Test
    public void resolve() throws NoSuchMethodException, NoSuchFieldException {
        //class
        assertEquals(C1.class,
                JavaCodeSerializer.resolveClass(MyTestModelStore.org.reflections.TestModel$C1.class));

        //method
        assertEquals(C4.class.getDeclaredMethod("m1"),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.reflections.TestModel$C4.methods.m1.class));

        //overloaded method with parameters
        assertEquals(C4.class.getDeclaredMethod("m1", int.class, String[].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.reflections.TestModel$C4.methods.m1_int__java_lang_String$$.class));

        //overloaded method with parameters and multi dimensional array
        assertEquals(C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                JavaCodeSerializer.resolveMethod(MyTestModelStore.org.reflections.TestModel$C4.methods.m1_int$$$$__java_lang_String$$$$.class));

        //field
        assertEquals(C4.class.getDeclaredField("f1"),
                JavaCodeSerializer.resolveField(MyTestModelStore.org.reflections.TestModel$C4.fields.f1.class));

        //annotation
        assertEquals(C2.class.getAnnotation(AC2.class),
                JavaCodeSerializer.resolveAnnotation(MyTestModelStore.org.reflections.TestModel$C2.annotations.org_reflections_TestModel$AC2.class));
    }
}
