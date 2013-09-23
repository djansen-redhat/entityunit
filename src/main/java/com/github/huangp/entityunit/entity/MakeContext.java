package com.github.huangp.entityunit.entity;

import com.github.huangp.entityunit.holder.BeanValueHolder;
import com.github.huangp.entityunit.maker.PreferredValueMakersRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor
@Getter
public class MakeContext {
    private final BeanValueHolder beanValueHolder;
    private final PreferredValueMakersRegistry preferredValueMakers;

}
