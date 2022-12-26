/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

/**
 * Source that can be bound by a {@link Binder}.
 * Bindable是指可由Binder绑定的源（如：基本数据类型，java对象、List、数组等等），也可以理解为可以绑定到指定的属性配置的值
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 * @since 2.0.0
 */
public final class Bindable<T> {

	//默认注解空数组
	private static final Annotation[] NO_ANNOTATIONS = {};

	private static final EnumSet<BindRestriction> NO_BIND_RESTRICTIONS = EnumSet.noneOf(BindRestriction.class);
	//要绑定项的类型
	private final ResolvableType type;

	//要绑定项的包装类型
	private final ResolvableType boxedType;

	//要绑定的数据值的提供者，是一个函数式接口
	private final Supplier<T> value;

	//可能影响绑定的任何关联注解
	private final Annotation[] annotations;

	private final EnumSet<BindRestriction> bindRestrictions;

	//私有的构造函数，创建一个Bindable实例
	private Bindable(ResolvableType type, ResolvableType boxedType, Supplier<T> value, Annotation[] annotations,
					 EnumSet<BindRestriction> bindRestrictions) {
		this.type = type;
		this.boxedType = boxedType;
		this.value = value;
		this.annotations = annotations;
		this.bindRestrictions = bindRestrictions;
	}

	/**
	 * Return the type of the item to bind.
	 * @return the type being bound
	 */
	public ResolvableType getType() {
		return this.type;
	}

	/**
	 * Return the boxed type of the item to bind.
	 * @return the boxed type for the item being bound
	 */
	public ResolvableType getBoxedType() {
		return this.boxedType;
	}

	/**
	 * Return a supplier that provides the object value or {@code null}.
	 * @return the value or {@code null}
	 */
	public Supplier<T> getValue() {
		return this.value;
	}

	/**
	 * Return any associated annotations that could affect binding.
	 * @return the associated annotations
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Return a single associated annotations that could affect binding.
	 * @param <A> the annotation type
	 * @param type annotation type
	 * @return the associated annotation or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		for (Annotation annotation : this.annotations) {
			if (type.isInstance(annotation)) {
				return (A) annotation;
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} if the specified bind restriction has been added.
	 * @param bindRestriction the bind restriction to check
	 * @return if the bind restriction has been added
	 * @since 2.5.0
	 */
	public boolean hasBindRestriction(BindRestriction bindRestriction) {
		return this.bindRestrictions.contains(bindRestriction);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Bindable<?> other = (Bindable<?>) obj;
		boolean result = true;
		result = result && nullSafeEquals(this.type.resolve(), other.type.resolve());
		result = result && nullSafeEquals(this.annotations, other.annotations);
		result = result && nullSafeEquals(this.bindRestrictions, other.bindRestrictions);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.bindRestrictions);
		return result;
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("type", this.type);
		creator.append("value", (this.value != null) ? "provided" : "none");
		creator.append("annotations", this.annotations);
		return creator.toString();
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	/**
	 * Create an updated {@link Bindable} instance with the specified annotations.
	 * 创建一个指定注解的更新Bindable实例对象
	 *
	 * @param annotations the annotations
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withAnnotations(Annotation... annotations) {
		return new Bindable<>(this.type, this.boxedType, this.value,
				(annotations != null) ? annotations : NO_ANNOTATIONS, NO_BIND_RESTRICTIONS);
	}

	/**
	 * Create an updated {@link Bindable} instance with an existing value.
	 * 创建一个存在指定值的更新Bindable实例对象
	 * @param existingValue the existing value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withExistingValue(T existingValue) {
		Assert.isTrue(
				existingValue == null || this.type.isArray() || this.boxedType.resolve().isInstance(existingValue),
				() -> "ExistingValue must be an instance of " + this.type);
		Supplier<T> value = (existingValue != null) ? () -> existingValue : null;
		return new Bindable<>(this.type, this.boxedType, value, this.annotations, this.bindRestrictions);
	}

	/**
	 * Create an updated {@link Bindable} instance with a value supplier.
	 * 创建一个suppliedValue值提供者更新的Bindable实例对象
	 * @param suppliedValue the supplier for the value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withSuppliedValue(Supplier<T> suppliedValue) {
		return new Bindable<>(this.type, this.boxedType, suppliedValue, this.annotations, this.bindRestrictions);
	}

	/**
	 * Create an updated {@link Bindable} instance with additional bind restrictions.
	 * 创建具有附加绑定限制的更新的Bindable实例。
	 * @param additionalRestrictions any additional restrictions to apply
	 * @return an updated {@link Bindable}
	 * @since 2.5.0
	 */
	public Bindable<T> withBindRestrictions(BindRestriction... additionalRestrictions) {
		EnumSet<BindRestriction> bindRestrictions = EnumSet.copyOf(this.bindRestrictions);
		bindRestrictions.addAll(Arrays.asList(additionalRestrictions));
		return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, bindRestrictions);
	}

	/**
	 * Create a new {@link Bindable} of the type of the specified instance with an
	 * existing value equal to the instance.
	 * 创建一个指定值及数据类型和指定值数据类型相同的Bindable实例对象
	 * @param <T> the source type
	 * @param instance the instance (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 * @see #withExistingValue(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bindable<T> ofInstance(T instance) {
		Assert.notNull(instance, "Instance must not be null");
		Class<T> type = (Class<T>) instance.getClass();
		return of(type).withExistingValue(instance);
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * 创建一个指定数据类型的Bindable实例对象
	 * @param <T> the source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 */
	public static <T> Bindable<T> of(Class<T> type) {
		Assert.notNull(type, "Type must not be null");
		return of(ResolvableType.forClass(type));
	}

	/**
	 * Create a new {@link Bindable} {@link List} of the specified element type.
	 * 创建一个指定数据类型List的Bindable实例对象
	 * @param <E> the element type
	 * @param elementType the list element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(List.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Set} of the specified element type.
	 * 创建一个指定数据类型Set的Bindable实例对象
	 * @param <E> the element type
	 * @param elementType the set element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(Set.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Map} of the specified key and value type.
	 * 创建一个指定数据类型Map的Bindable实例对象
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keyType the map key type
	 * @param valueType the map value type
	 * @return a {@link Bindable} instance
	 */
	public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
		return of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * 建一个指定类型的Bindable实例对象
	 *
	 * @param <T>  the source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(Class)
	 */
	public static <T> Bindable<T> of(ResolvableType type) {
		Assert.notNull(type, "Type must not be null");
		ResolvableType boxedType = box(type);
		return new Bindable<>(type, boxedType, null, NO_ANNOTATIONS, NO_BIND_RESTRICTIONS);
	}

	/**
	 * 将指定的数据类型转换为包装类型
	 *
	 * @param type
	 * @return
	 */
	private static ResolvableType box(ResolvableType type) {
		Class<?> resolved = type.resolve();
		if (resolved != null && resolved.isPrimitive()) {
			Object array = Array.newInstance(resolved, 1);
			Class<?> wrapperType = Array.get(array, 0).getClass();
			return ResolvableType.forClass(wrapperType);
		}
		if (resolved != null && resolved.isArray()) {
			return ResolvableType.forArrayComponent(box(type.getComponentType()));
		}
		return type;
	}

	/**
	 * Restrictions that can be applied when binding values.
	 * 绑定值时可以应用的限制
	 *
	 * @since 2.5.0
	 */
	public enum BindRestriction {

		/**
		 * Do not bind direct {@link ConfigurationProperty} matches.
		 */
		NO_DIRECT_PROPERTY

	}

}
