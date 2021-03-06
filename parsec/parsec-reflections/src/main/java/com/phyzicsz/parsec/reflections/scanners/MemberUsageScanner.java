package com.phyzicsz.parsec.reflections.scanners;

import com.phyzicsz.parsec.reflections.exception.ReflectionsException;
import com.phyzicsz.parsec.reflections.Store;
import com.phyzicsz.parsec.reflections.util.ClasspathUtils;
import com.phyzicsz.parsec.reflections.util.Utils;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

/**
 * Scans methods/constructors/fields usage.
 * 
 * <p><i> depends on
 * {@link com.phyzicsz.parsec.reflections.adapters.JavassistAdapter} configured </i>
 */
public class MemberUsageScanner extends AbstractScanner {

    private ClassPool classPool;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void scan(Object cls, Store store) {
        try {
            CtClass ctClass = getClassPool().get(getMetadataAdapter().getClassName(cls));
            for (CtBehavior member : ctClass.getDeclaredConstructors()) {
                scanMember(member, store);
            }
            for (CtBehavior member : ctClass.getDeclaredMethods()) {
                scanMember(member, store);
            }
            ctClass.detach();
        } catch (CannotCompileException | NotFoundException e) {
            throw new ReflectionsException("Could not scan method usage for " 
                    + getMetadataAdapter().getClassName(cls), e);
        }
    }

    void scanMember(CtBehavior member, Store store) throws CannotCompileException {
        //key contains this$/val$ means local field/parameter closure
        final String key = member.getDeclaringClass().getName() + "." + member.getMethodInfo().getName()
                + "(" + parameterNames(member.getMethodInfo()) + ")"; //+ " #" + member.getMethodInfo().getLineNumber(0)
        member.instrument(new ExprEditor() {
            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                try {
                    put(store, e.getConstructor().getDeclaringClass().getName() + "." + "<init>"
                            + "(" + parameterNames(e.getConstructor().getMethodInfo()) + ")", e.getLineNumber(), key);
                } catch (NotFoundException e1) {
                    throw new ReflectionsException("Could not find new instance usage in " + key, e1);
                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    put(store, m.getMethod().getDeclaringClass().getName() + "." + m.getMethodName()
                            + "(" + parameterNames(m.getMethod().getMethodInfo()) + ")", m.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + m.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                try {
                    put(store, c.getConstructor().getDeclaringClass().getName() + "." + "<init>"
                            + "(" + parameterNames(c.getConstructor().getMethodInfo()) + ")", c.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + c.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    put(store, f.getField().getDeclaringClass().getName() 
                            + "." 
                            + f.getFieldName(), f.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + f.getFieldName() + " in " + key, e);
                }
            }
        });
    }

    private void put(Store store, String key, int lineNumber, String value) {
        if (acceptResult(key)) {
            put(store, key, value + " #" + lineNumber);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    String parameterNames(MethodInfo info) {
        return Utils.join(getMetadataAdapter().getParameterNames(info), ", ");
    }

    private ClassPool getClassPool() {
        if (classPool == null) {
            synchronized (this) {
                classPool = new ClassPool();
                ClassLoader[] classLoaders = getConfiguration().getClassLoaders();
                if (classLoaders == null) {
                    classLoaders = ClasspathUtils.classLoaders();
                }
                for (ClassLoader classLoader : classLoaders) {
                    classPool.appendClassPath(new LoaderClassPath(classLoader));
                }
            }
        }
        return classPool;
    }
}
