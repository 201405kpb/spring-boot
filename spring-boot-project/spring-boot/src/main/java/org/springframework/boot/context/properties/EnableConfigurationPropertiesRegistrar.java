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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions
		.getQualifiedAttributeName(EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		// 注册基础设置Bean
		registerInfrastructureBeans(registry);
		registerMethodValidationExcludeFilter(registry);
		//创建注册器代理类
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
		//获取@EnableConfigurationProperties注解参数指定的配置类,并将其注册成Bean,beanName为 " prefix+配置类全类名"。
		getTypes(metadata).forEach(beanRegistrar::register);
	}


	//获取注解@EnableConfigurationProperties指定的被@ConfigurationProperties注解标注的bean实例对象
	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		return metadata.getAnnotations()
			.stream(EnableConfigurationProperties.class)
			.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
			.filter((type) -> void.class != type)
			.collect(Collectors.toSet());
	}

	//注册相关后置处理器和Bean用于注定绑定
	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
		//将BeanPostProcessor bean后置处理器ConfigurationPropertiesBindingPostProcessor注册到IOC容器
		ConfigurationPropertiesBindingPostProcessor.register(registry);
		//将提供属性绑定的BoundConfigurationProperties类注册到IOC容器之中
		BoundConfigurationProperties.register(registry);
	}

	static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
		//判断 MethodValidationExcludeFilter 是否已经注册
		if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(MethodValidationExcludeFilter.class, "byAnnotation")
				.addConstructorArgValue(ConfigurationProperties.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
		}
	}

}
