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

package org.springframework.boot.web.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.*;

/**
 * Abstract base {@link ServletContextInitializer} to register {@link Filter}s in a
 * Servlet 3.0+ container.
 *
 * @param <T> the type of {@link Filter} to register
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 1.5.22
 */
public abstract class AbstractFilterRegistrationBean<T extends Filter> extends DynamicRegistrationBean<Dynamic> {

	//默认的URL映射路径
	private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

	private Set<ServletRegistrationBean<?>> servletRegistrationBeans = new LinkedHashSet<>();

	private Set<String> servletNames = new LinkedHashSet<>();

	private Set<String> urlPatterns = new LinkedHashSet<>();

	private EnumSet<DispatcherType> dispatcherTypes;

	private boolean matchAfter = false;

	/**
	 * Create a new instance to be registered with the specified
	 * {@link ServletRegistrationBean}s.
	 * 创建AbstractFilterRegistrationBean实例对象，参数为ServletRegistrationBean集合
	 *
	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
	 */
	AbstractFilterRegistrationBean(ServletRegistrationBean<?>... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
	}

	/**
	 * Set {@link ServletRegistrationBean}s that the filter will be registered against.
	 * 设置ServletRegistrationBean，过滤器将会针对其进行注册
	 * @param servletRegistrationBeans the Servlet registration beans
	 */
	public void setServletRegistrationBeans(Collection<? extends ServletRegistrationBean<?>> servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
		this.servletRegistrationBeans = new LinkedHashSet<>(servletRegistrationBeans);
	}

	/**
	 * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
	 * will be registered against. {@link ServletRegistrationBean}s.
	 *  返回Filter简要根据ServletRegistrationBean集合对象进行注册的ServletRegistrationBean集合
	 * @return the Servlet registration beans
	 * @see #setServletNames
	 * @see #setUrlPatterns
	 */
	public Collection<ServletRegistrationBean<?>> getServletRegistrationBeans() {
		return this.servletRegistrationBeans;
	}

	/**
	 * Add {@link ServletRegistrationBean}s for the filter.
	 * 添加ServletRegistrationBean对象
	 * @param servletRegistrationBeans the servlet registration beans to add
	 * @see #setServletRegistrationBeans
	 */
	public void addServletRegistrationBeans(ServletRegistrationBean<?>... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
	}

	/**
	 * Set servlet names that the filter will be registered against. This will replace any
	 * previously specified servlet names.
	 * 设置过滤器将注册的servlet名称，这将替换以前指定的任何servlet名称
	 * @param servletNames the servlet names
	 * @see #setServletRegistrationBeans
	 * @see #setUrlPatterns
	 */
	public void setServletNames(Collection<String> servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames = new LinkedHashSet<>(servletNames);
	}

	/**
	 * Return a mutable collection of servlet names that the filter will be registered against.
	 * 返回用于注册过滤器的servlet名称的可变集合
	 * @return the servlet names
	 */
	public Collection<String> getServletNames() {
		return this.servletNames;
	}

	/**
	 * Add servlet names for the filter.
	 * 为Filter添加servlet名称
	 * @param servletNames the servlet names to add
	 */
	public void addServletNames(String... servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames.addAll(Arrays.asList(servletNames));
	}

	/**
	 * Set the URL patterns that the filter will be registered against. This will replace
	 * any previously specified URL patterns.
	 * 设置将根据其注册Filter的URL模式，这将替换以前指定的任何URL模式
	 * @param urlPatterns the URL patterns
	 * @see #setServletRegistrationBeans
	 * @see #setServletNames
	 */
	public void setUrlPatterns(Collection<String> urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		this.urlPatterns = new LinkedHashSet<>(urlPatterns);
	}

	/**
	 * Return a mutable collection of URL patterns, as defined in the Servlet
	 * specification, that the filter will be registered against.
	 * 返回一个URL模式的可变集合，如Servlet规范中定义的那样，过滤器针对这些模式进行注册
	 * @return the URL patterns
	 */
	public Collection<String> getUrlPatterns() {
		return this.urlPatterns;
	}

	/**
	 * Add URL patterns, as defined in the Servlet specification, that the filter will be
	 * registered against.
	 * 添加URL模式，如Servlet规范中所定义的，过滤器将针对这些模式进行注册
	 * @param urlPatterns the URL patterns
	 */
	public void addUrlPatterns(String... urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		Collections.addAll(this.urlPatterns, urlPatterns);
	}

	/**
	 * Convenience method to {@link #setDispatcherTypes(EnumSet) set dispatcher types}
	 * using the specified elements.
	 * @param first the first dispatcher type
	 * @param rest additional dispatcher types
	 */
	public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
		this.dispatcherTypes = EnumSet.of(first, rest);
	}

	/**
	 * Sets the dispatcher types that should be used with the registration. If not
	 * specified the types will be deduced based on the value of
	 * {@link #isAsyncSupported()}.
	 * @param dispatcherTypes the dispatcher types
	 */
	public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

	/**
	 * Set if the filter mappings should be matched after any declared filter mappings of
	 * the ServletContext. Defaults to {@code false} indicating the filters are supposed
	 * to be matched before any declared filter mappings of the ServletContext.
	 * @param matchAfter if filter mappings are matched after
	 */
	public void setMatchAfter(boolean matchAfter) {
		this.matchAfter = matchAfter;
	}

	/**
	 * Return if filter mappings should be matched after any declared Filter mappings of
	 * the ServletContext.
	 * @return if filter mappings are matched after
	 */
	public boolean isMatchAfter() {
		return this.matchAfter;
	}

	@Override
	protected String getDescription() {
		Filter filter = getFilter();
		Assert.notNull(filter, "Filter must not be null");
		return "filter " + getOrDeduceName(filter);
	}

	//注册过滤器
	@Override
	protected Dynamic addRegistration(String description, ServletContext servletContext) {
		Filter filter = getFilter();
		return servletContext.addFilter(getOrDeduceName(filter), filter);
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 * 配置过滤器的配置
	 * @param registration the registration
	 */
	@Override
	protected void configure(FilterRegistration.Dynamic registration) {
		super.configure(registration);
		EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
		if (dispatcherTypes == null) {
			T filter = getFilter();
			if (ClassUtils.isPresent("org.springframework.web.filter.OncePerRequestFilter",
					filter.getClass().getClassLoader()) && filter instanceof OncePerRequestFilter) {
				dispatcherTypes = EnumSet.allOf(DispatcherType.class);
			}
			else {
				dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
			}
		}
		Set<String> servletNames = new LinkedHashSet<>();
		for (ServletRegistrationBean<?> servletRegistrationBean : this.servletRegistrationBeans) {
			servletNames.add(servletRegistrationBean.getServletName());
		}
		servletNames.addAll(this.servletNames);
		if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, DEFAULT_URL_MAPPINGS);
		}
		else {
			if (!servletNames.isEmpty()) {
				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter,
						StringUtils.toStringArray(servletNames));
			}
			if (!this.urlPatterns.isEmpty()) {
				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
						StringUtils.toStringArray(this.urlPatterns));
			}
		}
	}

	/**
	 * Return the {@link Filter} to be registered.
	 * @return the filter
	 */
	public abstract T getFilter();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getOrDeduceName(this));
		if (this.servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
		}
		else {
			if (!this.servletNames.isEmpty()) {
				builder.append(" servlets=").append(this.servletNames);
			}
			if (!this.urlPatterns.isEmpty()) {
				builder.append(" urls=").append(this.urlPatterns);
			}
		}
		builder.append(" order=").append(getOrder());
		return builder.toString();
	}

}
