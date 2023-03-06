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

package org.springframework.boot.web.servlet;

import jakarta.servlet.Registration;
import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.BeanNameAware;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for Servlet 3.0+ {@link jakarta.servlet.Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @param <D> the dynamic registration result
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class DynamicRegistrationBean<D extends Registration.Dynamic> extends RegistrationBean
		implements BeanNameAware {

	private static final Log logger = LogFactory.getLog(RegistrationBean.class);

	//注册的名称，如果没有指定，将使用bean的名称
	private String name;

	//是否支持异步注册
	private boolean asyncSupported = true;

	private Map<String, String> initParameters = new LinkedHashMap<>();

	private String beanName;

	private boolean ignoreRegistrationFailure;

	/**
	 * Set the name of this registration. If not specified the bean name will be used.
	 * 设置注册的名称，如果没有指定，将使用bean的名称
	 *
	 * @param name the name of the registration
	 */
	public void setName(String name) {
		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
	}

	/**
	 * Sets if asynchronous operations are supported for this registration. If not
	 * specified defaults to {@code true}.
	 * 如果此操作支持异步注册，则支持异步集，如果未指定，则默认为true
	 * @param asyncSupported if async is supported
	 */
	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	/**
	 * Returns if asynchronous operations are supported for this registration.
	 * 判定当前注册是否支持异步注册
	 * @return if async is supported
	 */
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	/**
	 * Set init-parameters for this registration. Calling this method will replace any
	 * existing init-parameters.
	 * 为此注册设置init参数，调用此方法将替换任何现有的init参数
	 * @param initParameters the init parameters
	 * @see #getInitParameters
	 * @see #addInitParameter
	 */
	public void setInitParameters(Map<String, String> initParameters) {
		Assert.notNull(initParameters, "InitParameters must not be null");
		this.initParameters = new LinkedHashMap<>(initParameters);
	}

	/**
	 * Returns a mutable Map of the registration init-parameters.
	 * 返回注册的初始化参数
	 * @return the init parameters
	 */
	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	/**
	 * Add a single init-parameter, replacing any existing parameter with the same name.
	 * 添加一个init参数，用相同的名称替换任何现有的参数
	 * @param name the init-parameter name
	 * @param value the init-parameter value
	 */
	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Name must not be null");
		this.initParameters.put(name, value);
	}

	// 注册方法的具体实现
	@Override
	protected final void register(String description, ServletContext servletContext) {
		// 抽象方法，交由子类实现
		D registration = addRegistration(description, servletContext);
		if (registration == null) {
			if (this.ignoreRegistrationFailure) {
				logger.info(StringUtils.capitalize(description) + " was not registered (possibly already registered?)");
				return;
			}
			throw new IllegalStateException(
					"Failed to register '%s' on the servlet context. Possibly already registered?"
						.formatted(description));
		}
		// 设置初始化参数，也就是设置 `Map<String, String> initParameters` 参数
		configure(registration);
	}

	/**
	 * Sets whether registration failures should be ignored. If set to true, a failure
	 * will be logged. If set to false, an {@link IllegalStateException} will be thrown.
	 * @param ignoreRegistrationFailure whether to ignore registration failures
	 * @since 3.1.0
	 */
	public void setIgnoreRegistrationFailure(boolean ignoreRegistrationFailure) {
		this.ignoreRegistrationFailure = ignoreRegistrationFailure;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected abstract D addRegistration(String description, ServletContext servletContext);

	//注册结果及设置参数
	protected void configure(D registration) {
		registration.setAsyncSupported(this.asyncSupported);
		if (!this.initParameters.isEmpty()) {
			registration.setInitParameters(this.initParameters);
		}
	}

	/**
	 * Deduces the name for this registration. Will return user specified name or fallback
	 * to the bean name. If the bean name is not available, convention based naming is
	 * used.
	 * 推断此注册的名称，将返回用户指定的名称或回退到基于约定的命名
	 * @param value the object used for convention based names
	 * @return the deduced name
	 */
	protected final String getOrDeduceName(Object value) {
		if (this.name != null) {
			return this.name;
		}
		if (this.beanName != null) {
			return this.beanName;
		}
		return Conventions.getVariableName(value);
	}

}
