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

package org.springframework.boot.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Internal class used by the {@link ConfigurationPropertiesBindingPostProcessor} to
 * handle the actual {@link ConfigurationProperties @ConfigurationProperties} binding.
 * ConfigurationPropertiesBindingPostProcessor用于处理实际ConfigurationPropertis@ConfigurationProperties绑定的内部类。
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBinder {

	/**
	 * ConfigurationPropertiesBinder 对象名称
	 */
	private static final String BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinder";

	/**
	 * ConfigurationPropertiesBinderFactory 对象名称
	 */
	private static final String FACTORY_BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinderFactory";

	/**
	 * Validator 对象名称
	 */
	private static final String VALIDATOR_BEAN_NAME = EnableConfigurationProperties.VALIDATOR_BEAN_NAME;

	private final ApplicationContext applicationContext;

	private final PropertySources propertySources;

	private final Validator configurationPropertiesValidator;

	private final boolean jsr303Present;

	private volatile Validator jsr303Validator;

	private volatile Binder binder;

	ConfigurationPropertiesBinder(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.propertySources = new PropertySourcesDeducer(applicationContext).getPropertySources();
		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(applicationContext);
		this.jsr303Present = ConfigurationPropertiesJsr303Validator.isJsr303Present(applicationContext);
	}

	BindResult<?> bind(ConfigurationPropertiesBean propertiesBean) {
		Bindable<?> target = propertiesBean.asBindTarget();
		ConfigurationProperties annotation = propertiesBean.getAnnotation();
		// 获取 bindHandler 对象
		BindHandler bindHandler = getBindHandler(target, annotation);
		return getBinder().bind(annotation.prefix(), target, bindHandler);
	}

	Object bindOrCreate(ConfigurationPropertiesBean propertiesBean) {
		Bindable<?> target = propertiesBean.asBindTarget();
		ConfigurationProperties annotation = propertiesBean.getAnnotation();
		BindHandler bindHandler = getBindHandler(target, annotation);
		return getBinder().bindOrCreate(annotation.prefix(), target, bindHandler);
	}

	private Validator getConfigurationPropertiesValidator(ApplicationContext applicationContext) {
		if (applicationContext.containsBean(VALIDATOR_BEAN_NAME)) {
			return applicationContext.getBean(VALIDATOR_BEAN_NAME, Validator.class);
		}
		return null;
	}

	private <T> BindHandler getBindHandler(Bindable<T> target, ConfigurationProperties annotation) {
		// 获取校验器集合，没有任何的校验器，直接返回空的集合
		List<Validator> validators = getValidators(target);
		// 获取handler,实际上是 IgnoreTopLevelConverterNotFoundBindHandler 类型
		BindHandler handler = getHandler();
		// 将handler 包装为 ConfigurationPropertiesBindHandler 类型对象
		handler = new ConfigurationPropertiesBindHandler(handler);
		//注解属性处理，若属性值绑定到字段，发生错误时，忽略异常，则将handler 包装为 IgnoreErrorsBindHandler 类型对象
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		//注配置项向实体类中的属性绑定时，没有找到对应的字段，不忽略异常，则将handler 包装为 NoUnboundElementsBindHandler 类型对象
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		// 若校验器集合不为空，则将 handler 封装为 ValidationBindHandler 类型对象
		if (!validators.isEmpty()) {
			handler = new ValidationBindHandler(handler, validators.toArray(new Validator[0]));
		}
		// 应用附件功能
		for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
			handler = advisor.apply(handler);
		}
		// 返回对象
		return handler;
	}

	/**
	 * 获取默认的 BindHandler 对象
	 */
	private IgnoreTopLevelConverterNotFoundBindHandler getHandler() {
		BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.applicationContext);
		return (bound != null)
				? new IgnoreTopLevelConverterNotFoundBindHandler(new BoundPropertiesTrackingBindHandler(bound::add))
				: new IgnoreTopLevelConverterNotFoundBindHandler();
	}

	/**
	 * 获取检验器集合
	 */
	private List<Validator> getValidators(Bindable<?> target) {
		List<Validator> validators = new ArrayList<>(3);
		if (this.configurationPropertiesValidator != null) {
			validators.add(this.configurationPropertiesValidator);
		}
		if (this.jsr303Present && target.getAnnotation(Validated.class) != null) {
			validators.add(getJsr303Validator());
		}
		if (target.getValue() != null && target.getValue().get() instanceof Validator validator) {
			validators.add(validator);
		}
		return validators;
	}

	/**
	 * 获取 Jsr303 验证器
	 *
	 * @return
	 */
	private Validator getJsr303Validator() {
		if (this.jsr303Validator == null) {
			this.jsr303Validator = new ConfigurationPropertiesJsr303Validator(this.applicationContext);
		}
		return this.jsr303Validator;
	}

	/**
	 * 获取spring 容器中 ConfigurationPropertiesBindHandlerAdvisor 类型的对象集合，并将对象集合根据order 进行排序
	 */
	private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
		return this.applicationContext.getBeanProvider(ConfigurationPropertiesBindHandlerAdvisor.class)
			.orderedStream()
			.toList();
	}

	/**
	 * 获取 binder 对象
	 */
	private Binder getBinder() {
		if (this.binder == null) {
			this.binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
					getConversionServices(), getPropertyEditorInitializer(), null, null);
		}
		return this.binder;
	}

	private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
		return ConfigurationPropertySources.from(this.propertySources);
	}

	private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
		return new PropertySourcesPlaceholdersResolver(this.propertySources);
	}

	private List<ConversionService> getConversionServices() {
		return new ConversionServiceDeducer(this.applicationContext).getConversionServices();
	}

	private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
		if (this.applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			return configurableContext.getBeanFactory()::copyRegisteredEditorsTo;
		}
		return null;
	}

	static void register(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(ConfigurationPropertiesBinderFactory.class)
				.getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
		}
	}

	static ConfigurationPropertiesBinder get(BeanFactory beanFactory) {
		return beanFactory.getBean(BEAN_NAME, ConfigurationPropertiesBinder.class);
	}

	/**
	 * {@link BindHandler} to deal with
	 * {@link ConfigurationProperties @ConfigurationProperties} concerns.
	 */
	private static class ConfigurationPropertiesBindHandler extends AbstractBindHandler {

		ConfigurationPropertiesBindHandler(BindHandler handler) {
			super(handler);
		}

		@Override
		public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
			// 这个属性当然没有被 ConfigurationProperties 注解 ，所以没有增加他的限制属性 NO_DIRECT_PROPERTY（！！）
			return isConfigurationProperties(target.getType().resolve())
					? target.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY) : target;
		}

		private boolean isConfigurationProperties(Class<?> target) {
			return target != null && MergedAnnotations.from(target).isPresent(ConfigurationProperties.class);
		}

	}

	/**
	 * {@link FactoryBean} to create the {@link ConfigurationPropertiesBinder}.
	 */
	static class ConfigurationPropertiesBinderFactory
			implements FactoryBean<ConfigurationPropertiesBinder>, ApplicationContextAware {

		private ConfigurationPropertiesBinder binder;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.binder = (this.binder != null) ? this.binder : new ConfigurationPropertiesBinder(applicationContext);
		}

		@Override
		public Class<?> getObjectType() {
			return ConfigurationPropertiesBinder.class;
		}

		@Override
		public ConfigurationPropertiesBinder getObject() throws Exception {
			Assert.state(this.binder != null, "Binder was not created due to missing setApplicationContext call");
			return this.binder;
		}

	}

}
