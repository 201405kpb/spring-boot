/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link ImportBeanDefinitionRegistrar} for registering
 * {@link ConfigurationProperties @ConfigurationProperties} bean definitions via scanning.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesScanRegistrar implements ImportBeanDefinitionRegistrar {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	ConfigurationPropertiesScanRegistrar(Environment environment, ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		//获取包扫描范围,默认扫描@ConfigurationPropertiesScan所在类的包和子包
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
		//执行包扫描,只扫描被@ConfigurationProperties标记的类
		scan(registry, packagesToScan);
	}

	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		// 获取 ConfigurationPropertiesScan 注解对应的属性信息
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ConfigurationPropertiesScan.class.getName()));
		// 获取 basePackages 属性
		String[] basePackages = attributes.getStringArray("basePackages");
		// 获取 basePackageClasses 属性值
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		// 创建 packagesToScan 对象，避免重复
		Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
		// 添加 basePackageClasses 中 class 对象所在的包
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		// 若 packagesToScan 为空，则添加 metadata 对应类所在的包
		if (packagesToScan.isEmpty()) {
			packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
		}
		// 移除空值
		packagesToScan.removeIf((candidate) -> !StringUtils.hasText(candidate));
		return packagesToScan;
	}

	private void scan(BeanDefinitionRegistry registry, Set<String> packages) {
		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
		ClassPathScanningCandidateComponentProvider scanner = getScanner(registry);
		for (String basePackage : packages) {
			for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
				register(registrar, candidate.getBeanClassName());
			}
		}
	}

	private ClassPathScanningCandidateComponentProvider getScanner(BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.setEnvironment(this.environment);
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
		TypeExcludeFilter typeExcludeFilter = new TypeExcludeFilter();
		typeExcludeFilter.setBeanFactory((BeanFactory) registry);
		scanner.addExcludeFilter(typeExcludeFilter);
		return scanner;
	}

	private void register(ConfigurationPropertiesBeanRegistrar registrar, String className) throws LinkageError {
		try {
			register(registrar, ClassUtils.forName(className, null));
		}
		catch (ClassNotFoundException ex) {
			// Ignore
		}
	}

	private void register(ConfigurationPropertiesBeanRegistrar registrar, Class<?> type) {
		if (!isComponent(type)) {
			registrar.register(type);
		}
	}

	private boolean isComponent(Class<?> type) {
		return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).isPresent(Component.class);
	}

}
