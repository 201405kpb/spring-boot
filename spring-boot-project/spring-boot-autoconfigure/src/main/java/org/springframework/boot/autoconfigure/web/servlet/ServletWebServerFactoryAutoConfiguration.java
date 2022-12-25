/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.function.Supplier;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.WebListenerRegistrar;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for servlet web servers.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class) //存在 ServletRequest 类
@ConditionalOnWebApplication(type = Type.SERVLET)// WEB项目类型 为 SERVLET 类型
@EnableConfigurationProperties(ServerProperties.class)// 引入 ServerProperties 配置项
// 导入 内部类 BeanPostProcessorsRegistrar用于注册 BeanPostProcessor
// 导入 ServletWebServerFactoryConfiguration 三个内部类用于判断WEB应用服务类型
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
		ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
		ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
		ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {

	// 初始化 ServletWebServerFactoryCustomizer
	@Bean
	public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,
			ObjectProvider<WebListenerRegistrar> webListenerRegistrars,
			ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers) {
		return new ServletWebServerFactoryCustomizer(serverProperties, webListenerRegistrars.orderedStream().toList(),
				cookieSameSiteSuppliers.orderedStream().toList());
	}

	// 初始化 TomcatServletWebServerFactoryCustomizer
	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
	public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
			ServerProperties serverProperties) {
		return new TomcatServletWebServerFactoryCustomizer(serverProperties);
	}


	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
	@ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
	static class ForwardedHeaderFilterConfiguration {

		//初始化 ForwardedHeaderFilterCustomizer
		@Bean
		@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
		ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
			return (filter) -> filter.setRelativeRedirects(serverProperties.getTomcat().isUseRelativeRedirects());
		}

		// 实例化注册FilterRegistrationBean<ForwardedHeaderFilter>并设置其DispatcherType类型和优先级
		@Bean
		FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
				ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider) {
			ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
			customizerProvider.ifAvailable((customizer) -> customizer.customize(filter));
			FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return registration;
		}

	}

	interface ForwardedHeaderFilterCustomizer {

		void customize(ForwardedHeaderFilter filter);

	}

	/**
	 * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
	 * {@link ImportBeanDefinitionRegistrar} for early registration.
	 * 通过 ImportBeanDefinitionRegistrar 注册一个 WebServerFactoryCustomizerBeanPostProcessor
	 */
	public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		// 实现 BeanFactoryAware 的方法，设置 BeanFactory 属性
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
				this.beanFactory = listableBeanFactory;
			}
		}

		// 注册WebServerFactoryCustomizerBeanPostProcessor 与ErrorPageRegistrarBeanPostProcessor
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",
					WebServerFactoryCustomizerBeanPostProcessor.class,
					WebServerFactoryCustomizerBeanPostProcessor::new);
			registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class, ErrorPageRegistrarBeanPostProcessor::new);
		}
		// 检查并注册Bean对象
		private <T> void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry, String name,
				Class<T> beanClass, Supplier<T> instanceSupplier) {
			// 检查指定Bean 是否存在，如果不存在则创建Bean 对象并注册
			if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, instanceSupplier);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
