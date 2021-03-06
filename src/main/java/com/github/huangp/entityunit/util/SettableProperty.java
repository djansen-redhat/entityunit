package com.github.huangp.entityunit.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang
 */
public class SettableProperty implements Settable {
    private final Optional<Field> optionalField;
    private final Method getterMethod;

    private final transient String simpleName;
    private final transient String fullName;
    private final transient Type propertyType;

    private SettableProperty(Class ownerType, PropertyDescriptor propertyDescriptor) {
        Preconditions.checkArgument(propertyDescriptor.getReadMethod() != null || propertyDescriptor.getPropertyType() != null,
                "%s has no getter or field. Did you misspell the name?", propertyDescriptor.getName());
        simpleName = propertyDescriptor.getName();
        fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), simpleName);

        optionalField = findField(ownerType, simpleName);

        getterMethod = propertyDescriptor.getReadMethod();
        propertyType = getGenericType(propertyDescriptor);
    }

    private Type getGenericType(PropertyDescriptor propDesc) {
        if (getterMethod != null) {
            return getterMethod.getGenericReturnType();
        }
        else if (optionalField.isPresent()) {
            return optionalField.get().getGenericType();
        }
        return propDesc.getPropertyType();
    }

    private static Optional<Field> findField(Class ownerType, final String fieldName) {
        return Iterables.tryFind(ClassUtil.getAllDeclaredFields(ownerType), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.getName().equals(fieldName);
            }
        });
    }

    public static Settable from(Class ownerType, PropertyDescriptor propertyDescriptor) {
        return new SettableProperty(ownerType, propertyDescriptor);
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public Type getType() {
        return propertyType;
    }

    @Override
    public String fullyQualifiedName() {
        return fullName;
    }

    @Override
    public <T> T valueIn(Object ownerInstance) {
        return ClassUtil.invokeGetter(ownerInstance, getterMethod);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return optionalField.isPresent() && optionalField.get().isAnnotationPresent(annotationClass)
                || getterMethod.isAnnotationPresent(annotationClass);

    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T annotation = null;
        if (optionalField.isPresent()) {
            annotation = optionalField.get().getAnnotation(annotationClass);
        }
        return annotation != null ? annotation : getterMethod.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        if (optionalField.isPresent()) {
            return ObjectArrays.concat(optionalField.get().getAnnotations(), getterMethod.getAnnotations(), Annotation.class);
        }
        return getterMethod.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        if (optionalField.isPresent()) {
            return ObjectArrays.concat(optionalField.get().getDeclaredAnnotations(), getterMethod.getDeclaredAnnotations(), Annotation.class);
        }
        return getterMethod.getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return fullName;
    }
}
