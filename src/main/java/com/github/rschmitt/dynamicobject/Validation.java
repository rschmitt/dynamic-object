package com.github.rschmitt.dynamicobject;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

class Validation {
    @SuppressWarnings("unchecked")
    static void validateCollection(Collection<?> val, Type genericReturnType) {
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
            assert typeArgs.size() == 1;

            Type typeArg = typeArgs.get(0);
            if (typeArg instanceof WildcardType)
                throw new UnsupportedOperationException("Wildcard return types are not supported");
            else if (typeArg instanceof ParameterizedType)
                throw new UnsupportedOperationException("Nested type parameters are not supported");
            else if (typeArg instanceof Class)
                val.forEach(element -> checkElement((Class<?>) typeArg, element));
            else
                throw new UnsupportedOperationException("Unknown generic type argument type: " + typeArg.getClass().getCanonicalName());
        }
    }

    private static void checkElement(Class<?> expectedElementType, Object element) {
        if (element != null) {
            if (!expectedElementType.isAssignableFrom(element.getClass()))
                throw new IllegalStateException(format("Expected collection element of type %s, got %s",
                        expectedElementType.getTypeName(),
                        element.getClass().getTypeName()));
            if (element instanceof DynamicObject)
                ((DynamicObject) element).validate();
        }
    }

    static String getValidationErrorMessage(Collection<Method> missingFields, Map<Method, Class<?>> mismatchedFields) {
        StringBuilder ret = new StringBuilder();
        if (!missingFields.isEmpty()) {
            ret.append("The following @Required fields were missing: ");
            List<String> fieldNames = missingFields.stream().map(Method::getName).collect(toList());
            for (int i = 0; i < fieldNames.size(); i++) {
                ret.append(fieldNames.get(i));
                if (i != fieldNames.size() - 1)
                    ret.append(", ");
            }
            ret.append("\n");
        }
        if (!mismatchedFields.isEmpty()) {
            ret.append("The following fields had the wrong type:\n");
            for (Map.Entry<Method, Class<?>> methodClassEntry : mismatchedFields.entrySet()) {
                Method method = methodClassEntry.getKey();
                String name = method.getName();
                String expected = method.getReturnType().getSimpleName();
                String actual = methodClassEntry.getValue().getSimpleName();
                ret.append(format("\t%s (expected %s, got %s)%n", name, expected, actual));
            }
        }
        return ret.toString();
    }
}
