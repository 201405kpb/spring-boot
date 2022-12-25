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

package org.springframework.boot.web.servlet;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*' (unless
 * {@link #ServletRegistrationBean(Servlet, boolean, String...) alwaysMapUrl} is set to
 * {@code false}). The servlet name will be deduced if not specified.
 *
 * @param <T> the type of the {@link Servlet} to register
 * @author Phillip Webb
 * @since 1.4.0
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 */
public class ServletRegistrationBean<T extends Servlet> extends DynamicRegistrationBean<ServletRegistration.Dynamic> {

	//默认路径匹配
	private static final String[] DEFAULT_MAPPINGS = { "/*" };

	//将要注册的servlet
	private T servlet;

	//映射的URL路由模式集合
	private Set<String> urlMappings = new LinkedHashSet<>();

	//如果省略了URL映射，则应将其替换为"/*"
	private boolean alwaysMapUrl = true;

	//启动优先级
	private int loadOnStartup = -1;

	//要设置的配置
	private MultipartConfigElement multipartConfig;

	/**
	 * Create a new {@link ServletRegistrationBean} instance.
	 * 创建一个ServletRegistrationBean实例对象
	 */
	public ServletRegistrationBean() {
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * 创建一个ServletRegistrationBean实例对象，并且制定servlet和URL映射参数
	 * {@link Servlet} and URL mappings.
	 * @param servlet the servlet being mapped
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(T servlet, String... urlMappings) {
		this(servlet, true, urlMappings);
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * 创建一个ServletRegistrationBean实例对象，并且制定servlet、如果省略URL则使用"/*"替换,和URL映射参数
	 * {@link Servlet} and URL mappings.
	 * @param servlet the servlet being mapped
	 * @param alwaysMapUrl if omitted URL mappings should be replaced with '/*'
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(T servlet, boolean alwaysMapUrl, String... urlMappings) {
		Assert.notNull(servlet, "Servlet must not be null");
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.servlet = servlet;
		this.alwaysMapUrl = alwaysMapUrl;
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the servlet to be registered.
	 * @param servlet the servlet
	 */
	public void setServlet(T servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
	}

	/**
	 * Return the servlet being registered.
	 * 返回正在注册的servlet
	 * @return the servlet
	 */
	public T getServlet() {
		return this.servlet;
	}

	/**
	 * Set the URL mappings for the servlet. If not specified the mapping will default to
	 * '/'. This will replace any previously specified mappings.
	 * 设置servlet的URL映射，如果未指定，映射将默认为"/",这将替换以前指定的任何映射
	 * @param urlMappings the mappings to set
	 * @see #addUrlMappings(String...)
	 */
	public void setUrlMappings(Collection<String> urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings = new LinkedHashSet<>(urlMappings);
	}

	/**
	 * Return a mutable collection of the URL mappings, as defined in the Servlet
	 * specification, for the servlet.
	 * 返回注册的servlet的映射集合
	 * @return the urlMappings
	 */
	public Collection<String> getUrlMappings() {
		return this.urlMappings;
	}

	/**
	 * Add URL mappings, as defined in the Servlet specification, for the servlet.
	 * 添加注册的servlet的url映射
	 * @param urlMappings the mappings to add
	 * @see #setUrlMappings(Collection)
	 */
	public void addUrlMappings(String... urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the {@code loadOnStartup} priority. See
	 * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
	 * 设置loadOnStartup方法的优先级
	 * @param loadOnStartup if load on startup is enabled
	 */
	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	/**
	 * Set the {@link MultipartConfigElement multi-part configuration}.
	 * 设置注册servlet的配置
	 * @param multipartConfig the multipart configuration to set or {@code null}
	 */
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		this.multipartConfig = multipartConfig;
	}

	/**
	 * Returns the {@link MultipartConfigElement multi-part configuration} to be applied
	 * or {@code null}.
	 * 获取servlet的配置
	 * @return the multipart config
	 */
	public MultipartConfigElement getMultipartConfig() {
		return this.multipartConfig;
	}

	//获取注册servlet的描述
	@Override
	protected String getDescription() {
		Assert.notNull(this.servlet, "Servlet must not be null");
		return "servlet " + getServletName();
	}

	//核心，向ServletContext注册servlet对象
	@Override
	protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
		// 获取 Servlet 的名称
		String name = getServletName();
		// 将该 Servlet 添加至 ServletContext 上下文中
		return servletContext.addServlet(name, this.servlet);
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 * 配置注册配置
	 * @param registration the registration
	 */
	@Override
	protected void configure(ServletRegistration.Dynamic registration) {
		super.configure(registration);
		// 设置需要拦截的 URL，默认 `/*`
		String[] urlMapping = StringUtils.toStringArray(this.urlMappings);
		if (urlMapping.length == 0 && this.alwaysMapUrl) {
			urlMapping = DEFAULT_MAPPINGS;
		}
		if (!ObjectUtils.isEmpty(urlMapping)) {
			registration.addMapping(urlMapping);
		}
		// 设置需要加载的优先级
		registration.setLoadOnStartup(this.loadOnStartup);
		if (this.multipartConfig != null) {
			registration.setMultipartConfig(this.multipartConfig);
		}
	}

	/**
	 * Returns the servlet name that will be registered.
	 * 获取将被注册的servlet的名字
	 * @return the servlet name
	 */
	public String getServletName() {
		return getOrDeduceName(this.servlet);
	}

	@Override
	public String toString() {
		return getServletName() + " urls=" + getUrlMappings();
	}

}
