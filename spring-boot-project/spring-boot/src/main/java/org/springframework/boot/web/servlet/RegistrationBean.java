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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Base class for Servlet 3.0+ based registration beans.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see ServletRegistrationBean
 * @see FilterRegistrationBean
 * @see DelegatingFilterProxyRegistrationBean
 * @see ServletListenerRegistrationBean
 */
public abstract class RegistrationBean implements ServletContextInitializer, Ordered {

	private static final Log logger = LogFactory.getLog(RegistrationBean.class);

	//注册bean的优先级
	private int order = Ordered.LOWEST_PRECEDENCE;

	//指示注册是否已经启用的标记
	private boolean enabled = true;

	@Override
	public final void onStartup(ServletContext servletContext) throws ServletException {
		//获取注册bean的描述信息
		String description = getDescription();
		//判定是否开启注册功能，否则打印info日志并且直接返回
		if (!isEnabled()) {
			logger.info(StringUtils.capitalize(description) + " was not registered (disabled)");
			return;
		}
		//调用抽象的注册方法
		register(description, servletContext);
	}

	/**
	 * Return a description of the registration. For example "Servlet resourceServlet"
	 *
	 * @return a description of the registration 返回注册的描述说明
	 */
	protected abstract String getDescription();

	/**
	 * Register this bean with the servlet context. 在servlet上下文中注册这个bean.
	 * @param description a description of the item being registered 描述信息
	 * @param servletContext the servlet context
	 */
	protected abstract void register(String description, ServletContext servletContext);

	/**
	 * Flag to indicate that the registration is enabled.
	 * 指示注册是否已经启用的标记
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Return if the registration is enabled.
	 * 返回注册是否一起用的标记boolean值
	 * @return if enabled (default {@code true})
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Set the order of the registration bean.
	 * 设置注册bean的优先级顺序
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Get the order of the registration bean.
	 * 获取注册bean的优先级顺序
	 * @return the order
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

}
