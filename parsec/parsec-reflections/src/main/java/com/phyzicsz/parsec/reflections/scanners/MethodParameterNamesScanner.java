package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.Store;
import com.phyzicsz.parsec.reflections.adapters.MetadataAdapter;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * Scans methods/constructors and indexes parameter names.
 * 
 */
public class MethodParameterNamesScanner extends AbstractScanner {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void scan(Object cls, Store store) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {
            String key = md.getMethodFullKey(cls, method);
            if (acceptResult(key)) {
                CodeAttribute codeAttribute = ((MethodInfo) method).getCodeAttribute();
                LocalVariableAttribute table = codeAttribute != null 
                        ? (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag) : null;
                int length = md.getParameterNames(method).size();
                if (length > 0) {
                    int shift = Modifier.isStatic(((MethodInfo) method).getAccessFlags()) ? 0 : 1; //skip this
                    String join = IntStream.range(shift, length + shift)
                            .mapToObj(i -> ((MethodInfo) method).getConstPool().getUtf8Info(table.nameIndex(i)))
                            .filter(name -> !name.startsWith("this$"))
                            .collect(Collectors.joining(", "));
                    if (!join.isEmpty()) {
                        put(store, key, join);
                    }
                }
            }
        }
    }
}
