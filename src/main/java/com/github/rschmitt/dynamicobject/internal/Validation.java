package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;

import javax.annotation.CheckReturnValue;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.invoke.MethodType.methodType;
import static java.util.stream.Collectors.toList;

class Validation {

    static <T extends DynamicObject<T>> MethodHandle buildValidatorFor(Class<T> klass) throws Exception {
        return new ValidationBuilder<T>(klass).buildValidator().asType(methodType(klass, klass));
    }

    static class ValidationBuilder<T extends DynamicObject<T>> {
        private static final MethodHandles.Lookup PRIVATE_LOOKUP = MethodHandles.lookup();

        private final Class<T> dynamicObjectType;

        ValidationBuilder(Class<T> dynamicObjectType) {
            this.dynamicObjectType = dynamicObjectType;
        }

        // returns MH of type DynamicObject(DynamicObjectInstance)
        public MethodHandle buildValidator() throws Exception {
            List<FieldValidator> validators = new ArrayList<>();
            Collection<Method> fields = Reflection.fieldGetters(dynamicObjectType);

            try {
                for (Method field : fields) {
                    FieldInfo info = new FieldInfo(field);

                    validators.add(buildValidator(info));
                }

                MethodHandle runCheck = PRIVATE_LOOKUP.findStatic(
                        ValidationBuilder.class, "doValidate",
                        MethodType.methodType(DynamicObject.class, DynamicObjectInstance.class, List.class)
                );

                return MethodHandles.insertArguments(runCheck, 1, validators);
            } catch (Exception e) {
                // Something is wrong with the class definition itself, so return a handle that always throws
                return throwException(
                        () -> {
                            if (e instanceof UnsupportedOperationException) {
                                return new UnsupportedOperationException(e.getMessage(), e);
                            } else {
                                return new UnsupportedOperationException(
                                        "Validation failed due to structural error in DynamicObject type",
                                        e
                                );
                            }
                        },
                        methodType(DynamicObject.class, DynamicObjectInstance.class)
                );
            }
        }

        @SuppressWarnings("unused") // invoked via reflection
        private static DynamicObject<?> doValidate(
                DynamicObjectInstance<?> instance,
                List<FieldValidator> validators
        ) {
            ValidationResult result = new ValidationResult();

            for (FieldValidator validator : validators) {
                validator.validate(instance, result);
            }

            result.checkResult();

            return instance.$$customValidate();
        }

        private FieldValidator buildValidator(FieldInfo info) throws Exception {
            // First, build a method handle that will perform the get, check for nulls, and perform a cast check if
            // non-null
            MethodHandle isNull = PRIVATE_LOOKUP.findStatic(
                    Objects.class, "isNull", methodType(Boolean.TYPE, Object.class)
            );

            MethodHandle castChecker = MethodHandles.identity(Object.class);
            castChecker = castChecker.asType(methodType(info.boxedType, info.boxedType));
            // This second asType does not undo the first - we'll still attempt the cast to object and back.
            castChecker = castChecker.asType(methodType(Object.class, Object.class));

            MethodHandle castCheckIfNotNull = MethodHandles.guardWithTest(
                    isNull, MethodHandles.identity(Object.class), castChecker
            );

            MethodHandle getAndCache = PRIVATE_LOOKUP.findVirtual(
                    DynamicObjectInstance.class,
                    "getAndCacheValueFor",
                    methodType(Object.class, Object.class, Type.class)
            );

            getAndCache = MethodHandles.insertArguments(getAndCache, 1, info.key, info.genericType);

            if (Reflection.getRawType(info.genericType) == Optional.class) {
                // Unbox the Optional by invoking .orElse(null)
                MethodHandle orElse = PRIVATE_LOOKUP.findVirtual(
                        Optional.class,
                        "orElse",
                        methodType(Object.class, Object.class)
                );

                orElse = MethodHandles.insertArguments(orElse, 1, new Object[] { null });
                orElse = orElse.asType(methodType(Object.class, Object.class));

                getAndCache = MethodHandles.filterReturnValue(getAndCache, orElse);
            }

            MethodHandle checkedGet = MethodHandles.filterReturnValue(getAndCache, castCheckIfNotNull);

            Consumer<Object> valueChecker = buildValueChecker(info.genericType);

            return (instance, result) -> {
                Object value;
                try {
                    value = checkedGet.invokeExact((DynamicObjectInstance)instance);
                } catch (ClassCastException | AssertionError e) {
                    result.mismatchedFields.put(info.getter, instance.getMap().get(info.key).getClass());
                    return;
                } catch (RuntimeException | Error e) {
                    // usually some form of conversion error
                    throw e;
                } catch (Throwable t) {
                    throw new IllegalStateException("Unexpected exception", t);
                }

                if (value == null) {
                    if (info.isRequired) {
                        result.missingFields.add(info.getter);
                    }

                    return;
                }

                valueChecker.accept(value);
            };
        }

        private Consumer<Object> buildValueChecker(Type genericType) {
            if (!isSupportedGenericType(genericType)) {
                return throwingCheckerForUnsupportedType(genericType);
            }

            Class<?> rawType = Reflection.getRawType(genericType);

            if (DynamicObject.class.isAssignableFrom(rawType)) {
                return ValidationBuilder::checkDynamicObject;
            } else if (Map.class.isAssignableFrom(rawType)) {
                return buildMapChecker(genericType);
            } else if (Collection.class.isAssignableFrom(rawType)) {
                return buildCollectionChecker(genericType);
            } else {
                // no-op validator for types not known statically
                return value -> {};
            }
        }

        private Consumer<Object> buildMapChecker(Type genericType) {
            if (!(genericType instanceof ParameterizedType)) {
                return value -> {};
            }

            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
            assert typeArgs.size() == 2;

            Type keyType = typeArgs.get(0);
            Type valType = typeArgs.get(1);

            Consumer<Object> keyChecker = buildElementChecker(keyType);
            Consumer<Object> valChecker = buildElementChecker(valType);

            return value -> {
                Map<?, ?> m = (Map<?, ?>) value;

                m.forEach((k, v) -> {
                    keyChecker.accept(k);
                    valChecker.accept(v);
                });
            };
        }

        private Consumer<Object> buildCollectionChecker(Type genericType) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
                assert typeArgs.size() == 1;

                Type typeArg = typeArgs.get(0);

                Consumer<Object> elementChecker = buildElementChecker(typeArg);
                return value -> {
                    for (Object elem : (Collection)value) {
                        elementChecker.accept(elem);
                    }
                };
            } else {
                return value  -> {};
            }
        }

        private Consumer<Object> buildElementChecker(Type genericType) {
            if (!isSupportedGenericType(genericType)) {
                return throwingCheckerForUnsupportedType(genericType);
            }

            Class<?> rawType = Reflection.getRawType(genericType);
            Consumer<Object> checker = buildValueChecker(genericType);
            MethodHandle castCheck = castChecker(rawType);

            return value -> {
                if (value != null) {
                    try {
                        castCheck.invokeExact(value);
                    } catch (Throwable t) {
                        throw new IllegalStateException(format("Expected collection element of type %s, got %s",
                                                               rawType.getCanonicalName(),
                                                               value.getClass().getCanonicalName()));
                    }

                    checker.accept(value);
                }
            };
        }

        private static void checkDynamicObject(Object value) {
            ((DynamicObject)value).validate();
        }

        private Consumer<Object> throwingCheckerForUnsupportedType(Type genericType) {
            if (genericType instanceof WildcardType) {
                return value -> {
                    throw new UnsupportedOperationException("Wildcard return types are not supported");
                };
            } else {
                return value -> {
                    throw new UnsupportedOperationException("Unknown generic type argument type: "
                                                                    + genericType.getClass().getCanonicalName());
                };
            }
        }

        private boolean isSupportedGenericType(Type genericType) {
            return (genericType instanceof ParameterizedType) || (genericType instanceof Class);
        }

        @CheckReturnValue
        private Runnable checkTypeVariable(Type typeArg) {
            if (typeArg instanceof WildcardType)
                return () -> { throw new UnsupportedOperationException("Wildcard return types are not supported"); };
            else if (typeArg instanceof ParameterizedType)
                return () -> {};
            else if (typeArg instanceof Class)
                return () -> {};
            else
                throw new UnsupportedOperationException("Unknown generic type argument type: " + typeArg.getClass().getCanonicalName());
        }

        // Returns an void(Object) MethodHandle that will throw if its argument cannot be cast to the specified type,
        // and act as a no-op otherwise.
        private static MethodHandle castChecker(Class<?> castTo) {
            MethodHandle handle = MethodHandles.identity(castTo);
            return handle.asType(methodType(Void.TYPE, Object.class));
        }

        // Returns a methodhandle of the specified type that ignores its arguments and throws a new exception given
        // by the supplier specified.
        private static MethodHandle throwException(Supplier<Throwable> supplier, MethodType type) throws Exception {
            MethodHandle makeExn = PRIVATE_LOOKUP.findVirtual(Supplier.class, "get", methodType(Object.class));
            makeExn = makeExn.bindTo(supplier);
            makeExn = makeExn.asType(methodType(Throwable.class));

            MethodHandle throwExn = MethodHandles.throwException(type.returnType(), Throwable.class);
            throwExn = MethodHandles.foldArguments(throwExn, makeExn);

            // discard arguments specified by the provided type
            return MethodHandles.dropArguments(throwExn, 0, type.parameterArray());
        }
    }


    @FunctionalInterface
    private interface FieldValidator {
        void validate(DynamicObjectInstance<?> instance, ValidationResult result);
    }

    private static class FieldInfo {
        Method getter;
        Object key;
        Class<?> erasedType, boxedType;
        Type genericType;
        boolean isRequired;

        public FieldInfo(Method getter) {
            this.getter = getter;
            key = Reflection.getKeyForGetter(getter);
            genericType = getter.getGenericReturnType();
            erasedType = Reflection.getRawType(genericType);

            if (erasedType == Optional.class) {
                // We'll unbox the Optional later on, but need to remember the true generic type for the
                // initial get
                erasedType = Reflection.getRawType(
                        ((ParameterizedType)genericType).getActualTypeArguments()[0]
                );
            }

            boxedType = Primitives.box(erasedType);
            isRequired = Reflection.isRequired(getter);
        }
    }

    private static class ValidationResult {
        private final Collection<Method> missingFields = new LinkedHashSet<>();
        private final Map<Method, Class<?>> mismatchedFields = new HashMap<>();

        void checkResult() {
            if (!missingFields.isEmpty() || !mismatchedFields.isEmpty())
                throw new IllegalStateException(getValidationErrorMessage());
        }


        String getValidationErrorMessage() {
            StringBuilder ret = new StringBuilder();
            describeMissingFields(ret);
            describeMismatchedFields(ret);
            return ret.toString();
        }

        private void describeMismatchedFields(StringBuilder ret) {
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
        }

        private void describeMissingFields(StringBuilder ret) {
            if (!missingFields.isEmpty()) {
                ret.append("The following @Required fields were missing: ");
                List<String> fieldNames = missingFields.stream().map(Method::getName).collect(toList());
                ret.append(join(fieldNames));
                ret.append("\n");
            }
        }

        private static String join(List<String> strings) {
            return strings.stream().collect(Collectors.joining(", "));
        }

    }

}
