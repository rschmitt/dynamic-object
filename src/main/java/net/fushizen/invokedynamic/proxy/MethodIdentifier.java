package net.fushizen.invokedynamic.proxy;

import org.objectweb.asm.Type;

import java.util.Arrays;

class MethodIdentifier {
    private final String name;
    private final Class<?> returnType;
    private final Class<?>[] args;

    public MethodIdentifier(String name, Class<?> returnType, Class<?>[] args) {
        this.name = name;
        this.returnType = returnType;
        this.args = args.clone();
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        Type[] typeArgs = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            typeArgs[i] = Type.getType(args[i]);
        }

        return Type.getMethodType(
                Type.getType(returnType),
                typeArgs
        ).getDescriptor();
    }

    public Class<?>[] getArgs() {
        return args.clone();
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodIdentifier that = (MethodIdentifier) o;

        if (!Arrays.equals(args, that.args)) return false;
        if (!name.equals(that.name)) return false;
        if (!returnType.equals(that.returnType)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + returnType.hashCode();
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
