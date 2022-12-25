/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * {@link Conditional @Conditional} that matches when the application is a web
 * application. By default, any web application will match but it can be narrowed using
 * the {@link #type()} attribute.
 *
 * 当前是web环境
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnWebApplicationCondition.class)
public @interface ConditionalOnWebApplication {

	/**
	 * The required type of the web application.
	 * @return the required web application type
	 * 所需的WEB 应用程序类型
	 */
	Type type() default Type.ANY;

	/**
	 * Available application types.
	 * 可选应用程序枚举
	 */
	enum Type {

		/**
		 * Any web application will match.
		 * 任何类型
		 */
		ANY,

		/**
		 * Only servlet-based web application will match.
		 * 基于SERVLET 类型的WEB 应用
		 */
		SERVLET,

		/**
		 * Only reactive-based web application will match.
		 * 基于 REACTIVE 类型的WEB 应用
		 */
		REACTIVE

	}

}
