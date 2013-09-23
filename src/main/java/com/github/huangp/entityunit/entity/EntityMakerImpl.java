package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.maker.BeanMaker;
import com.github.huangp.entityunit.util.ClassUtil;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jodah.typetools.TypeResolver;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author Patrick Huang
 */
@Slf4j
class EntityMakerImpl implements EntityMaker {
    private final EntityClassScanner scanner;
    private final MakeContext context;
    private final BeanValueHolder valueHolder;

    EntityMakerImpl(EntityClassScanner scanner, MakeContext context) {
        this.scanner = scanner;
        this.context = context;
        valueHolder = context.getBeanValueHolder();
    }

    @Override
    public <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType) {
        return makeAndPersist(entityManager, entityType, AbstractNoOpCallback.NO_OP_CALLBACK);
    }

    @Override
    public <T> T makeAndPersist(EntityManager entityManager, Class<T> entityType, Callback callback) {
        Iterable<Object> allObjects = getRequiredEntitiesFor(entityType);

        entityManager.getTransaction().begin();

        Iterable<Object> toPersist = callback.beforePersist(entityManager, allObjects);
        persistInOrder(entityManager, toPersist);
        Iterable<Object> toReturn = callback.afterPersist(entityManager, toPersist);

        entityManager.getTransaction().commit();
        return ClassUtil.findEntity(toReturn, entityType);
    }

    private Iterable<Object> getRequiredEntitiesFor(Class askingClass) {
        Iterable<EntityClass> dependingEntities = scanner.scan(askingClass);
        Queue<Object> queue = Queues.newLinkedBlockingQueue();

        // create all depending (ManyToOne or required OneToOne) entities
        for (EntityClass entityClass : dependingEntities) {
            reuseOrMakeNew(queue, entityClass);
        }
        // we always make new asking class
        Serializable askingEntity = new BeanMaker<Serializable>(askingClass, context).value();

        context.getBeanValueHolder().putIfNotNull(askingClass, askingEntity);
        queue.offer(askingEntity);

        // now work backwards to fill in the one to many side
        for (EntityClass entityNode : dependingEntities) {
            Object entity = valueHolder.tryGet(entityNode.getType()).get();

            Iterable<Method> getterMethods = entityNode.getContainingEntitiesGetterMethods();
            for (Method method : getterMethods) {
                Type returnType = method.getGenericReturnType();
                if (ClassUtil.isCollection(returnType)) {
                    addManySideEntityIfExists(entity, method, valueHolder);
                }
                if (ClassUtil.isMap(returnType)) {
                    putManySideEntityIfExists(entity, method, valueHolder);
                }
            }
        }
        // required OneToOne mapping should have been set on entity creation
        // @see SingleEntityMaker
        // @see ReuseOrNullMaker

        log.debug("result {}", new NiceIterablePrinter(queue));
        return queue;
    }

    private void reuseOrMakeNew(Queue<Object> queue, EntityClass entityClass) {
        Optional existing = valueHolder.tryGet(entityClass.getType());
        if (!entityClass.isRequireNewInstance() && existing.isPresent()) {
            queue.offer(existing.get());
        } else {
            Serializable entity = new BeanMaker<Serializable>(entityClass.getType(), context).value();
            context.getBeanValueHolder().putIfNotNull(TypeToken.of(entityClass.getType()), entity);
            queue.offer(entity);
        }
    }

    private static void persistInOrder(EntityManager entityManager, Iterable<Object> queue) {
        for (Object entity : queue) {
            if (ClassUtil.isUnsaved(entity)) {
                entityManager.persist(entity);
            } else {
                log.info("reused persisted entity: {}", entity);
                //            entityManager.refresh(entity);
                //            entityManager.merge(entity);
            }
        }
    }

    @Override
    public BeanValueHolder exportCopyOfBeans() {
        return valueHolder.getCopy();
    }

    private static void addManySideEntityIfExists(Object entity, Method method, BeanValueHolder holder) {
        Class<?> genericType = TypeResolver.resolveRawArgument(method.getGenericReturnType(), Collection.class);
        Optional<?> manySideExists = holder.tryGet(genericType);
        if (manySideExists.isPresent()) {
            Object existValue = manySideExists.get();
            Collection collection = ClassUtil.invokeGetter(entity, method, Collection.class);
            if (collection != null) {
                collection.add(existValue);
            }
        }
    }

    private static void putManySideEntityIfExists(Object entity, Method method, BeanValueHolder holder) {
        Class<?>[] genericTypes = TypeResolver.resolveRawArguments(method.getGenericReturnType(), Collection.class);
        Class<?> keyType = genericTypes[0];
        Class<?> valueType = genericTypes[1];
        Optional keyOptional = holder.tryGet(keyType);
        Optional valueOptional = holder.tryGet(valueType);
        warningIfTrue(!keyOptional.isPresent(), "You have to manually resolve this: {} {}.{}()", entity, method);

        if (keyOptional.isPresent() && valueOptional.isPresent()) {
            Object key = keyOptional.get();
            Object value = valueOptional.get();
            Map map = ClassUtil.invokeGetter(entity, method, Map.class);
            if (map != null) {
                map.put(key, value);
            }
        }
    }

    private static void warningIfTrue(boolean expression, String logTemplate, Object entity, Method method) {
        if (expression) {
            log.warn(logTemplate, method.getGenericReturnType(), entity.getClass().getSimpleName(), method.getName());
        }
    }

    @RequiredArgsConstructor
    private static class NiceIterablePrinter {
        private static final String NEW_LINE = "\n";
        private static final String THEN = "    ==> ";
        private final Iterable<Object> iterable;

        @Override
        public String toString() {
            List<Object> entities = ImmutableList.copyOf(iterable);
            StringBuilder builder = new StringBuilder();
            builder.append(NEW_LINE);
            for (Object entity : entities) {
                builder.append(THEN).append(entity);
                builder.append(NEW_LINE);
            }
            return builder.toString();
        }
    }
}
