/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link DataObjectBinder} for mutable Java Beans.
 * JavaBeanBinder实现类是通过getter/setter绑定
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements DataObjectBinder {


	//JavaBeanBinder的空实例对象
	static final JavaBeanBinder INSTANCE = new JavaBeanBinder();

	//绑定指定的值到指定的属性
	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
					  DataObjectPropertyBinder propertyBinder) {
		boolean hasKnownBindableProperties = target.getValue() != null && hasKnownBindableProperties(name, context);
		//获取javabean的属性字段对应的绑定结果集
		Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
		//绑定字段值，并返回是否绑定成功
		boolean bound = bind(propertyBinder, bean, beanSupplier, context);
		return (bound ? beanSupplier.get() : null);
	}

	//创建属性字段值的实例
	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Bindable<T> target, Context context) {
		//获取属性值的class实例
		Class<T> type = (Class<T>) target.getType().resolve();
		//实例化属性字段类型
		return (type != null) ? BeanUtils.instantiateClass(type) : null;
	}

	private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	//将javabean字段属性值绑定为指定的配置属性
	private <T> boolean bind(DataObjectPropertyBinder propertyBinder, Bean<T> bean, BeanSupplier<T> beanSupplier,
							 Context context) {
		boolean bound = false;
		for (BeanProperty beanProperty : bean.getProperties().values()) {
			bound |= bind(beanSupplier, propertyBinder, beanProperty);
			context.clearConfigurationProperty();
		}
		return bound;
	}

	private <T> boolean bind(BeanSupplier<T> beanSupplier, DataObjectPropertyBinder propertyBinder,
							 BeanProperty property) {
		//获取属性字段名
		String propertyName = property.getName();
		//获取属性字段类型
		ResolvableType type = property.getType();
		//获取属性字段值的Supplier包装对象
		Supplier<Object> value = property.getValue(beanSupplier);
		//获取属性字段上标注的注解
		Annotation[] annotations = property.getAnnotations();
		//获取属性字段的绑定值
		Object bound = propertyBinder.bindProperty(propertyName,
				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
		if (bound == null) {
			return false;
		}
		//设置属性字段的值
		if (property.isSettable()) {
			property.setValue(beanSupplier, bound);
		} else if (value == null || !bound.equals(value.get())) {
			throw new IllegalStateException("No setter found for property: " + property.getName());
		}
		return true;
	}

	/**
	 * The bean being bound.
	 *
	 * @param <T> the bean type
	 */
	static class Bean<T> {
		//Bean实例对象
		private static Bean<?> cached;
		//JavaBean的数据类型
		private final ResolvableType type;
		//javabean的class实例对象
		private final Class<?> resolvedType;
		//javabean的属性字段及绑定值之间的映射关系
		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

		Bean(ResolvableType type, Class<?> resolvedType) {
			//初始化type javabean的数据类型
			this.type = type;
			//javabean的class实例对象
			this.resolvedType = resolvedType;
			addProperties(resolvedType);
		}

		private void addProperties(Class<?> type) {
			while (type != null && !Object.class.equals(type)) {
				//获取javabean的所有方法对象
				Method[] declaredMethods = getSorted(type, this::getDeclaredMethods, Method::getName);
				//获取javabean的所有属性字段对象
				Field[] declaredFields = getSorted(type, Class::getDeclaredFields, Field::getName);
				addProperties(declaredMethods, declaredFields);
				type = type.getSuperclass();
			}
		}

		private Method[] getDeclaredMethods(Class<?> type) {
			Method[] methods = type.getDeclaredMethods();
			Set<Method> result = new LinkedHashSet<>(methods.length);
			for (Method method : methods) {
				result.add(BridgeMethodResolver.findBridgedMethod(method));
			}
			return result.toArray(new Method[0]);
		}

		private <S, E> E[] getSorted(S source, Function<S, E[]> elements, Function<E, String> name) {
			E[] result = elements.apply(source);
			Arrays.sort(result, Comparator.comparing(name));
			return result;
		}

		protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
			for (int i = 0; i < declaredMethods.length; i++) {
				if (!isCandidate(declaredMethods[i])) {
					declaredMethods[i] = null;
				}
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
			}
			for (Field field : declaredFields) {
				addField(field);
			}
		}

		private boolean isCandidate(Method method) {
			int modifiers = method.getModifiers();
			return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers)
					&& !Modifier.isStatic(modifiers) && !method.isBridge()
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass()) && method.getName().indexOf('$') == -1;
		}

		private void addMethodIfPossible(Method method, String prefix, int parameterCount,
										 BiConsumer<BeanProperty, Method> consumer) {
			if (method != null && method.getParameterCount() == parameterCount && method.getName().startsWith(prefix)
					&& method.getName().length() > prefix.length()) {
				String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
				consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
			}
		}

		//获取BeanProperty实例对象，即指定属性及值的对应关系
		private BeanProperty getBeanProperty(String name) {
			return new BeanProperty(name, this.type);
		}

		//添加指定字段的Filed属性对象值
		private void addField(Field field) {
			BeanProperty property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		//获取javabean属性字段及值的集合
		Map<String, BeanProperty> getProperties() {
			return this.properties;
		}

		//获取绑定值的实例对象
		@SuppressWarnings("unchecked")
		BeanSupplier<T> getSupplier(Bindable<T> target) {
			return new BeanSupplier<>(() -> {
				T instance = null;
				if (target.getValue() != null) {
					instance = target.getValue().get();
				}
				if (instance == null) {
					instance = (T) BeanUtils.instantiateClass(this.resolvedType);
				}
				return instance;
			});
		}

		//获取当前类的实例对象
		@SuppressWarnings("unchecked")
		static <T> Bean<T> get(Bindable<T> bindable, boolean canCallGetValue) {
			ResolvableType type = bindable.getType();
			Class<?> resolvedType = type.resolve(Object.class);
			Supplier<T> value = bindable.getValue();
			T instance = null;
			if (canCallGetValue && value != null) {
				instance = value.get();
				resolvedType = (instance != null) ? instance.getClass() : resolvedType;
			}
			if (instance == null && !isInstantiable(resolvedType)) {
				return null;
			}
			Bean<?> bean = Bean.cached;
			if (bean == null || !bean.isOfType(type, resolvedType)) {
				bean = new Bean<>(type, resolvedType);
				cached = bean;
			}
			return (Bean<T>) bean;
		}

		private static boolean isInstantiable(Class<?> type) {
			if (type.isInterface()) {
				return false;
			}
			try {
				type.getDeclaredConstructor();
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		private boolean isOfType(ResolvableType type, Class<?> resolvedType) {
			if (this.type.hasGenerics() || type.hasGenerics()) {
				return this.type.equals(type);
			}
			return this.resolvedType != null && this.resolvedType.equals(resolvedType);
		}

	}

	private static class BeanSupplier<T> implements Supplier<T> {

		private final Supplier<T> factory;

		private T instance;

		BeanSupplier(Supplier<T> factory) {
			this.factory = factory;
		}

		@Override
		public T get() {
			if (this.instance == null) {
				this.instance = this.factory.get();
			}
			return this.instance;
		}

	}

	/**
	 * A bean property being bound.
	 */
	static class BeanProperty {

		//属性名
		private final String name;

		//属性配置源对应的java bean类类型实例对象
		private final ResolvableType declaringClassType;

		//属性对应的getter方法Method对象
		private Method getter;

		//属性对应的setter方法Method对象
		private Method setter;

		//属性字段的Field对象
		private Field field;

		//创建属性的BeanProperty实例对象
		BeanProperty(String name, ResolvableType declaringClassType) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.declaringClassType = declaringClassType;
		}

		//初始化getter对象
		void addGetter(Method getter) {
			if (this.getter == null || this.getter.getName().startsWith("is")) {
				this.getter = getter;
			}
		}

		void addSetter(Method setter) {
			if (this.setter == null || isBetterSetter(setter)) {
				this.setter = setter;
			}
		}

		//setter方法初始化条件判定
		private boolean isBetterSetter(Method setter) {
			return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
		}

		//初始化field对象
		void addField(Field field) {
			if (this.field == null) {
				this.field = field;
			}
		}

		//获取属性字段名
		String getName() {
			return this.name;
		}

		//获取字段的数据类型
		ResolvableType getType() {
			if (this.setter != null) {
				MethodParameter methodParameter = new MethodParameter(this.setter, 0);
				return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
			}
			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
			return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
		}

		//获取字段上标注的注解
		Annotation[] getAnnotations() {
			try {
				return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
			} catch (Exception ex) {
				return null;
			}
		}

		//获取字段对应的属性值
		Supplier<Object> getValue(Supplier<?> instance) {
			if (this.getter == null) {
				return null;
			}
			return () -> {
				try {
					this.getter.setAccessible(true);
					return this.getter.invoke(instance.get());
				} catch (Exception ex) {
					throw new IllegalStateException("Unable to get value for property " + this.name, ex);
				}
			};
		}

		//判定属性字段是否可以设置值
		boolean isSettable() {
			return this.setter != null;
		}

		//设置属性的值
		void setValue(Supplier<?> instance, Object value) {
			try {
				this.setter.setAccessible(true);
				//获取属性配置源对象，将指定的值设置为指定属性的值
				this.setter.invoke(instance.get(), value);
			} catch (Exception ex) {
				throw new IllegalStateException("Unable to set value for property " + this.name, ex);
			}
		}

	}

}
