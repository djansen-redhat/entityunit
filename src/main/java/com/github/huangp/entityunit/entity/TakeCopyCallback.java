package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.util.ClassUtil;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author Patrick Huang
 */
public class TakeCopyCallback extends AbstractNoOpCallback {
    @Getter
    private List<Object> copy;

    @Override
    public Iterable<Object> beforePersist(EntityManager entityManager, Iterable<Object> toBePersisted) {
        copy = ImmutableList.copyOf(toBePersisted);
        return toBePersisted;
    }

    public <T> T getByIndex(int index) {
        return (T) copy.get(index);
    }

    public <T> T getByType(Class<T> type) {
        return ClassUtil.findEntity(copy, type);
    }

}
