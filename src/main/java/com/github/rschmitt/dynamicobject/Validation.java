package com.github.rschmitt.dynamicobject;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

class Validation {
    static <T extends DynamicObject<T>> void validateInstance(
            Class<T> type,
            Function<Method, Object> getter,
            Function<Method, Object> rawGetter
    ) {
        Collection<Method> fields = Reflection.fieldGetters(type);
        Collection<Method> missingFields = new LinkedHashSet<>();
        Map<Method, Class<?>> mismatchedFields = new HashMap<>();
        for (Method field : fields) {
            try {
                Object val = getter.apply(field);
                if (Reflection.isRequired(field) && val == null)
                    missingFields.add(field);
                if (val != null) {
                    Type genericReturnType = field.getGenericReturnType();
                    if (val instanceof Optional && ((Optional) val).isPresent()) {
                        genericReturnType = Reflection.getTypeArgument(genericReturnType, 0);
                        val = ((Optional) val).get();
                    }
                    Class<?> expectedType = Primitives.box(Reflection.getRawType(genericReturnType));
                    Class<?> actualType = val.getClass();
                    if (!expectedType.isAssignableFrom(actualType))
                        mismatchedFields.put(field, actualType);
                    if (val instanceof DynamicObject)
                        ((DynamicObject) val).validate();
                    else if (val instanceof List || val instanceof Set)
                        Validation.validateCollection((Collection<?>) val, genericReturnType);
                    else if (val instanceof Map)
                        Validation.validateMap((Map<?, ?>) val, genericReturnType);
                }
            } catch (ClassCastException | AssertionError cce) {
                mismatchedFields.put(field, rawGetter.apply(field).getClass());
            }
        }
        if (!missingFields.isEmpty() || !mismatchedFields.isEmpty())
            throw new IllegalStateException(Validation.getValidationErrorMessage(missingFields, mismatchedFields));
    }

    @SuppressWarnings("unchecked")
    static void validateCollection(Collection<?> val, Type genericReturnType) {
        if (val == null) return;
        Class<?> baseCollectionType = getRawType(genericReturnType);
        if (!baseCollectionType.isAssignableFrom(val.getClass()))
            throw new IllegalStateException(format("Wrong collection type: expected %s, got %s",
                    baseCollectionType.getSimpleName(), val.getClass().getSimpleName()));
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
            assert typeArgs.size() == 1;

            Type typeArg = typeArgs.get(0);
            checkTypeVariable(typeArg);
            val.forEach(element -> checkElement(typeArg, element));
        }
    }

    private static void checkTypeVariable(Type typeArg) {
        if (typeArg instanceof WildcardType)
            throw new UnsupportedOperationException("Wildcard return types are not supported");
        else if (typeArg instanceof ParameterizedType)
            return;
        else if (typeArg instanceof Class)
            return;
        else
            throw new UnsupportedOperationException("Unknown generic type argument type: " + typeArg.getClass().getCanonicalName());
    }

    private static void checkElement(Type elementType, Object element) {
        if (elementType instanceof Class)
            checkAtomicElement((Class<?>) elementType, element);
        else
            checkNestedElement(elementType, element);
    }

    private static void checkAtomicElement(Class<?> elementType, Object element) {
        if (element != null) {
            Class<?> actualType = element.getClass();
            Class<?> expectedType = elementType;
            if (!expectedType.isAssignableFrom(actualType))
                throw new IllegalStateException(format("Expected collection element of type %s, got %s",
                        expectedType.getCanonicalName(),
                        actualType.getCanonicalName()));
            if (element instanceof DynamicObject)
                ((DynamicObject) element).validate();
        }
    }

    private static void checkNestedElement(Type elementType, Object element) {
        Class<?> rawType = getRawType(elementType);
        if (List.class.isAssignableFrom(rawType) || Set.class.isAssignableFrom(rawType))
            validateCollection((Collection<?>) element, elementType);
        else if (Map.class.isAssignableFrom(rawType))
            validateMap((Map<?, ?>) element, elementType);
        else
            throw new UnsupportedOperationException("Unsupported base type " + rawType.getCanonicalName());
    }

    static void validateMap(Map<?, ?> map, Type genericReturnType) {
        if (map == null) return;
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
            assert typeArgs.size() == 2;

            typeArgs.forEach(Validation::checkTypeVariable);
            Type keyType = typeArgs.get(0);
            Type valType = typeArgs.get(1);

            map.keySet().forEach(k -> checkElement(keyType, k));
            map.values().forEach(v -> checkElement(valType, v));
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

    private static Class<?> getRawType(Type genericType) {
        if (genericType instanceof Class)
            return (Class<?>) genericType;
        else if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            return (Class<?>) parameterizedType.getRawType();
        } else
            throw new UnsupportedOperationException();
    }
}
