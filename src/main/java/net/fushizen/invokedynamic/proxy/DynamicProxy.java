package net.fushizen.invokedynamic.proxy;

import org.objectweb.asm.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

/**
 * A proxy class built using Java 7 INVOKEDYNAMIC mechanics.
 */
public class DynamicProxy {
    private static final AtomicInteger CLASS_COUNT = new AtomicInteger();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final String BOOTSTRAP_DYNAMIC_METHOD_NAME = "$$bootstrapDynamic";
    public static final String BOOTSTRAP_DYNAMIC_METHOD_DESCRIPTOR
            = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/invoke/MethodHandle;)Ljava/lang/invoke/CallSite;";
    public static final String INIT_PROXY_METHOD_NAME = "$$initProxy";

    private final Class<?> proxyClass;
    private final MethodHandle constructor;

    private DynamicProxy(Class<?> proxyClass, MethodHandle constructor) {
        this.proxyClass = proxyClass;
        this.constructor = constructor;
    }

    /**
     *
     * @return the generated and loaded proxy class object
     */
    public Class<?> proxyClass() {
        return proxyClass;
    }

    /**
     *
     * @return A MethodHandle that will construct an instance of this proxy.
     */
    public MethodHandle constructor() {
        return constructor;
    }

    /**
     *
     * @return A Supplier that will construct an instance of this proxy.
     */
    public Supplier<Object> supplier() {
        return () -> {
            try {
                return constructor().invoke();
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Class<?> superclass = Object.class;
        private ArrayList<Class<?>> interfaces = new ArrayList<>();
        private DynamicInvocationHandler invocationHandler = new DefaultInvocationHandler();
        private boolean hasFinalizer = false;
        private String packageName;
        public String proxyNameHint;

        public Builder withProxyNameHint(String proxyNameHint) {
            this.proxyNameHint = proxyNameHint;
            return this;
        }

        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder withInterfaces(Class<?>... interfaces) {
            for (Class<?> klass : interfaces) {
                if (!klass.isInterface()) throw new IllegalArgumentException("" + klass + " is not an interface");

                if ((klass.getModifiers() & Modifier.PUBLIC) == 0) {
                    packageFromClass(klass);
                }
            }

            this.interfaces.addAll(Arrays.asList(interfaces));

            return this;
        }

        private void packageFromClass(Class<?> klass) {
            if ((klass.getModifiers() & Modifier.PRIVATE) != 0) {
                throw new IllegalArgumentException("Cannot extend private interface or superclass " + klass);
            }

            String klassPackage = klass.getPackage().getName();

            if (packageName != null && !packageName.equals(klassPackage)) {
                throw new IllegalArgumentException("Cannot access private interfaces or superclasses from multiple packages");
            }

            packageName = klassPackage;
        }

        public Builder withInvocationHandler(DynamicInvocationHandler handler) {
            invocationHandler = handler;

            return this;
        }

        /**
         * Requests that a proxy method for void finalize() should be generated. If this method is not called, a
         * finalize method will only be generated if it is found on a non-Object class of interest.
         */
        public Builder withFinalizer() {
            hasFinalizer = true;

            return this;
        }

        /**
         * Sets the superclass of the proxy. The superclass must be public, and must have a public zero-argument
         * constructor. If this method is not called, Object will be used.
         * @param klass
         * @return
         * @throws NoSuchMethodException If a suitable constructor is not found
         * @throws java.lang.IllegalArgumentException if klass is not a class
         */
        public Builder withSuperclass(Class<?> klass) throws NoSuchMethodException {
            if (klass.isInterface()) throw new IllegalArgumentException("" + klass + " is an interface");
            if ((klass.getModifiers() & Modifier.FINAL) != 0) throw new IllegalArgumentException("" + klass + " is final");

            if ((klass.getModifiers() & Modifier.PUBLIC) == 0) {
                packageFromClass(klass);
            }

            Constructor ctor = klass.getDeclaredConstructor();
            if ((ctor.getModifiers() & Modifier.PUBLIC) == 0) {
                if ((ctor.getModifiers() & Modifier.PRIVATE) != 0) {
                    throw new IllegalArgumentException("Constructor " + ctor + " is private");
                }

                packageFromClass(klass);
            }

            superclass = klass;

            return this;
        }

        public DynamicProxy build() throws Exception {
            Class<?> proxyClass = generateProxyClass(this);
            MethodHandle constructor = LOOKUP.findConstructor(proxyClass, MethodType.methodType(Void.TYPE));

            MethodHandle init = LOOKUP.findStatic(proxyClass, INIT_PROXY_METHOD_NAME, MethodType.methodType(Void.TYPE, DynamicInvocationHandler.class));

            try {
                init.invokeExact(invocationHandler);
            } catch (Throwable t) {
                throw new Error("Unexpected exception from proxy initializer", t);
            }

            return new DynamicProxy(proxyClass, constructor);
        }
    }

    private static Class<?> generateProxyClass(Builder builder) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        String packageInternalName = builder.packageName;
        if (packageInternalName == null) {
            packageInternalName = "net.fushizen.invokedynamic.proxy.generated";
        }
        packageInternalName = packageInternalName.replaceAll("\\.", "/");
        String proxyNameHint = builder.proxyNameHint;
        if (proxyNameHint == null) proxyNameHint = "";

        String classInternalName = String.format("%s/%sProxy$%d", packageInternalName, proxyNameHint, CLASS_COUNT.incrementAndGet());
        String superclassName = Type.getInternalName(builder.superclass);
        String[] interfaceNames = new String[builder.interfaces.size()];
        for (int i = 0; i < builder.interfaces.size(); i++) {
            interfaceNames[i] = Type.getInternalName(builder.interfaces.get(i));
        }

        cw.visit(V1_7,
                ACC_PUBLIC | ACC_FINAL,
                classInternalName,
                null,
                superclassName,
                interfaceNames);

        visitFields(cw, builder);
        visitCtor(cw, superclassName);
        visitInternalMethods(cw, classInternalName, builder);

        HashMap<MethodIdentifier, ArrayList<Method>> methods = new HashMap<>();
        for (Class<?> interfaceClass : builder.interfaces) {
            collectMethods(interfaceClass, builder, methods);
        }
        collectMethods(builder.superclass, builder, methods);

        visitMethods(cw, classInternalName, methods);

        cw.visitEnd();

        byte[] classData = cw.toByteArray();

        ProxyLoader loader = new ProxyLoader(DynamicProxy.class.getClassLoader());
        return loader.loadClass(classData);
    }

    private static void visitMethods(ClassVisitor cw, String classInternalName, HashMap<MethodIdentifier, ArrayList<Method>> methods) {
        methods.forEach((method, contributors) -> emitMethod(cw, classInternalName, method, contributors));
    }

    private static void emitMethod(
            ClassVisitor cw,
            String classInternalName,
            MethodIdentifier method,
            List<Method> contributors
    ) {
        int access = ACC_PROTECTED;
        ConcreteMethodTracker concreteMethodTracker = new ConcreteMethodTracker();

        for (Method m : contributors) {
            concreteMethodTracker.add(m);

            if (Modifier.PUBLIC == (m.getModifiers() & Modifier.PUBLIC)) {
                access = ACC_PUBLIC;
            }
        }

        MethodVisitor mv = cw.visitMethod(
                access,
                method.getName(),
                method.getDescriptor(),
                null,
                null
                );

        mv.visitCode();
        // All we really need to do here is:
        // 1) Get all of our arguments (including this) onto the stack
        // 2) invokedynamic
        // 3) Use the appropriate variant of return to return the result to the caller

        ArrayList<Type> descriptorArgs = new ArrayList<>();

        mv.visitVarInsn(ALOAD, 0); // load 'this'
        descriptorArgs.add(Type.getObjectType(classInternalName));

        int argIndex = 1;
        for (Class<?> argKlass : method.getArgs()) {
            switch (typeIdentifier(argKlass)) {
                case 'I':
                    mv.visitVarInsn(ILOAD, argIndex);
                    break;
                case 'L':
                    mv.visitVarInsn(LLOAD, argIndex);
                    argIndex++; // longs use two variable indexes
                    break;
                case 'F':
                    mv.visitVarInsn(FLOAD, argIndex);
                    break;
                case 'D':
                    mv.visitVarInsn(DLOAD, argIndex);
                    argIndex++; // doubles use two variable indexes
                    break;
                case 'A':
                    mv.visitVarInsn(ALOAD, argIndex);
                    break;
                default:
                    throw new UnsupportedOperationException(); // should never happen
            }
            argIndex++;

            descriptorArgs.add(Type.getType(argKlass));
        }

        Handle bootstrapHandle = new Handle(
                H_INVOKESTATIC,
                classInternalName,
                BOOTSTRAP_DYNAMIC_METHOD_NAME,
                BOOTSTRAP_DYNAMIC_METHOD_DESCRIPTOR
        );

        // We always need some kind of handle to pass, even if we don't have a supermethod. Pass the bootstrap handle in
        // this case (our bootstrap shim will null this out before calling user code)
        Handle superHandle = bootstrapHandle;
        Method superMethod = concreteMethodTracker.getOnlyContributor();
        int hasSuper = 0;

        if (superMethod != null) {
            hasSuper = 1;
            superHandle = new Handle(
                    H_INVOKESPECIAL,
                    Type.getInternalName(superMethod.getDeclaringClass()),
                    superMethod.getName(),
                    Type.getMethodDescriptor(superMethod)
            );
        }

        mv.visitInvokeDynamicInsn(
                method.getName(),
                Type.getMethodType(Type.getType(method.getReturnType()), descriptorArgs.toArray(new Type[0])).getDescriptor(),
                bootstrapHandle,
                (Integer)hasSuper,
                superHandle
        );

        switch (typeIdentifier(method.getReturnType())) {
            case 'V':
                mv.visitInsn(RETURN);
                break;
            case 'I':
                mv.visitInsn(IRETURN);
                break;
            case 'L':
                mv.visitInsn(LRETURN);
                break;
            case 'F':
                mv.visitInsn(FRETURN);
                break;
            case 'D':
                mv.visitInsn(DRETURN);
                break;
            case 'A':
                mv.visitInsn(ARETURN);
                break;
            default:
                throw new UnsupportedOperationException(); // should never happen
        }

        mv.visitMaxs(argIndex, argIndex);
        mv.visitEnd();
    }

    private static char typeIdentifier(Class<?> argKlass) {
        if (argKlass == Byte.TYPE
                || argKlass == Character.TYPE
                || argKlass == Short.TYPE
                || argKlass == Integer.TYPE
                || argKlass == Boolean.TYPE) {
            return 'I';
        } else if (argKlass == Long.TYPE) {
            return 'L';
        } else if (argKlass == Float.TYPE) {
            return 'F';
        } else if (argKlass == Double.TYPE) {
            return 'D';
        } else if (argKlass == Void.TYPE) {
            return 'V';
        } else {
            return 'A';
        }
    }

    private static void visitFields(ClassVisitor cw, Builder builder) {
        FieldVisitor fw = cw.visitField(
                ACC_PRIVATE | ACC_STATIC,
                "$$handler",
                Type.getDescriptor(DynamicInvocationHandler.class),
                null,
                null
        );
        fw.visitEnd();
    }

    private static void visitInternalMethods(ClassVisitor cw, String classBinaryName, Builder builder) {
        // This code is ASMified from ordinary java source.
        MethodVisitor mv;
        {
            /*
            public synchronized static void $$initProxy(DynamicInvocationHandler handler) {
                if ($$handler != null) {
                    throw new IllegalStateException();
                }

                $$handler = handler;
            }
            */
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNCHRONIZED,
                    INIT_PROXY_METHOD_NAME,
                    "(Lnet/fushizen/invokedynamic/proxy/DynamicInvocationHandler;)V",
                    null,
                    null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC,
                    classBinaryName,
                    "$$handler",
                    "Lnet/fushizen/invokedynamic/proxy/DynamicInvocationHandler;");
            Label l1 = new Label();
            mv.visitJumpInsn(IFNULL, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l1);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(PUTSTATIC,
                    classBinaryName,
                    "$$handler",
                    "Lnet/fushizen/invokedynamic/proxy/DynamicInvocationHandler;");
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitInsn(RETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLocalVariable("handler", "Lnet/fushizen/invokedynamic/proxy/DynamicInvocationHandler;", null, l0, l4, 0);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }

        {
            /*
                private static CallSite $$bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type, int hasSuper, MethodHandle superMethod) {
                    return $$handler.handleInvocation(caller, name, type, hasSuper != 0 ? superMethod : null);
                }
             */
            mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC,
                    BOOTSTRAP_DYNAMIC_METHOD_NAME,
                    BOOTSTRAP_DYNAMIC_METHOD_DESCRIPTOR,
                    null,
                    null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC,
                    classBinaryName,
                    "$$handler",
                    "Lnet/fushizen/invokedynamic/proxy/DynamicInvocationHandler;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);

            mv.visitVarInsn(ILOAD, 3);
            Label l1 = new Label();
            mv.visitJumpInsn(IFNE, l1);
            mv.visitInsn(ACONST_NULL);
            Label l2 = new Label();
            mv.visitJumpInsn(GOTO, l2);
            mv.visitLabel(l1);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitLabel(l2);

            mv.visitMethodInsn(INVOKEINTERFACE,
                    "net/fushizen/invokedynamic/proxy/DynamicInvocationHandler",
                    "handleInvocation",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/CallSite;",
                    true);

            mv.visitInsn(ARETURN);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitMaxs(5, 5);
            mv.visitEnd();
        }
    }

    private static void collectMethods(Class<?> klass, Builder builder, HashMap<MethodIdentifier, ArrayList<Method>> methods) {
        if (klass.getSuperclass() != null && klass != Object.class) {
            collectMethods(klass.getSuperclass(), builder, methods);
        }

        for (Class<?> interfaceClass : klass.getInterfaces()) {
            collectMethods(interfaceClass, builder, methods);
        }

        for (Method m : klass.getDeclaredMethods()) {
            if (0 == (m.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC))) {
                continue; // Private or package scope method
            }

            if (0 != (m.getModifiers() & Modifier.FINAL)) {
                continue; // Can't override FINAL methods
            }

            if (klass == Object.class && m.getName().equals("finalize") && !builder.hasFinalizer) {
                // Creating this method has a performance hit, so only do it if requested
                continue;
            }

            MethodIdentifier identifier = new MethodIdentifier(m.getName(), m.getReturnType(), m.getParameterTypes());
            ArrayList<Method> methodOwners = methods.get(identifier);

            if (methodOwners == null) {
                methodOwners = new ArrayList<>();
                methods.put(identifier, methodOwners);
            }

            methodOwners.add(m);
        }
    }

    private static void visitCtor(ClassVisitor cw, String superclassName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static class ProxyLoader extends ClassLoader {
        protected ProxyLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> loadClass(byte[] buf) {
            return defineClass(null, buf, 0, buf.length);
        }
    }
}
