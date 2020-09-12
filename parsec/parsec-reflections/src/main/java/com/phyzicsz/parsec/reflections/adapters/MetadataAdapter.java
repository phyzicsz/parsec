package com.phyzicsz.parsec.reflections.adapters;

import com.phyzicsz.parsec.reflections.vfs.Vfs;
import java.util.List;

/**
 * Interface for metadata adaptation.
 *
 * @param <C> class information
 * @param <F> field information
 * @param <M> method information
 */
public interface MetadataAdapter<C, F, M> {

    String getClassName(final C cls);

    String getSuperclassName(final C cls);

    List<String> getInterfacesNames(final C cls);

    List<F> getFields(final C cls);

    List<M> getMethods(final C cls);

    String getMethodName(final M method);

    List<String> getParameterNames(final M method);

    List<String> getClassAnnotationNames(final C classz);

    List<String> getFieldAnnotationNames(final F field);

    List<String> getMethodAnnotationNames(final M method);

    List<String> getParameterAnnotationNames(final M method, final int parameterIndex);

    String getReturnTypeName(final M method);

    String getFieldName(final F field);

    C getOrCreateClassObject(Vfs.File file) throws Exception;

    String getMethodModifier(M method);

    String getMethodKey(C cls, M method);

    String getMethodFullKey(C cls, M method);

    boolean isPublic(Object o);

    boolean acceptsInput(String file);

}
