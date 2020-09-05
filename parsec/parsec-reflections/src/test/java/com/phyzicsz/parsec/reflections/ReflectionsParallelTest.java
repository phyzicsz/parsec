package com.phyzicsz.parsec.reflections;


import com.phyzicsz.parsec.reflections.scanners.FieldAnnotationsScanner;
import com.phyzicsz.parsec.reflections.scanners.MemberUsageScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodAnnotationsScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodParameterNamesScanner;
import com.phyzicsz.parsec.reflections.scanners.MethodParameterScanner;
import com.phyzicsz.parsec.reflections.scanners.SubTypesScanner;
import com.phyzicsz.parsec.reflections.scanners.TypeAnnotationsScanner;
import com.phyzicsz.parsec.reflections.util.ClasspathHelper;
import com.phyzicsz.parsec.reflections.util.ConfigurationBuilder;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeAll
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner())
                .useParallelExecutor());
    }
}
