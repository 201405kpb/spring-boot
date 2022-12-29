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

package org.springframework.boot.web.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that sets {@link Environment} properties for the
 * ports that {@link WebServer} servers are actually listening on. The property
 * {@literal "local.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained via the {@link Environment}.
 * <p>
 * If the {@link WebServerInitializedEvent} has a
 * {@link WebServerApplicationContext#getServerNamespace() server namespace} , it will be
 * used to construct the property name. For example, the "management" actuator context
 * will have the property name {@literal "local.management.port"}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * ServerPortInfoApplicationContextInitializer是ApplicationContextInitializer接口的实现类，会通过SPI方式在应用程序启动时初始化，
 * 其主要作用是在环境Environment中添加一个属性源，将应用的本地端口号添加进去，方便通过@Value或environment获取本地端口号；
 * ServerPortInfoApplicationContextInitializer初始化器还实现了另外一个ApplicationListener监听器，监听器实现方法会在服务器server启动后调用。
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ServerPortInfoApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<WebServerInitializedEvent> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(this);
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		String propertyName = "local." + getName(event.getApplicationContext()) + ".port";
		setPortProperty(event.getApplicationContext(), propertyName, event.getWebServer().getPort());
	}

	private String getName(WebServerApplicationContext context) {
		String name = context.getServerNamespace();
		return StringUtils.hasText(name) ? name : "server";
	}

	private void setPortProperty(ApplicationContext context, String propertyName, int port) {
		if (context instanceof ConfigurableApplicationContext configurableContext) {
			setPortProperty(configurableContext.getEnvironment(), propertyName, port);
		}
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), propertyName, port);
		}
	}

	@SuppressWarnings("unchecked")
	private void setPortProperty(ConfigurableEnvironment environment, String propertyName, int port) {
		//获取environment环境中的属性源配置集合
		MutablePropertySources sources = environment.getPropertySources();
		//获取集合中name为server.ports的PropertySource属性源配置
		PropertySource<?> source = sources.get("server.ports");
		if (source == null) {
			//创建一个name是server.ports的字典类型属性源配置
			source = new MapPropertySource("server.ports", new HashMap<>());
			//将属性源配置放在最高优先级位置
			sources.addFirst(source);
		}
		//将端口属性名local.server.port及本地端口号添加到environment中
		((Map<String, Object>) source.getSource()).put(propertyName, port);
	}

}
