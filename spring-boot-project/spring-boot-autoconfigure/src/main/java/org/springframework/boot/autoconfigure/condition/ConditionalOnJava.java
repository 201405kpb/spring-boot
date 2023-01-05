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

import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * {@link Conditional @Conditional} that matches based on the JVM version the application
 * is running on.
 *
 * 系统的java版本是否符合要求
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJavaCondition.class)
public @interface ConditionalOnJava {

	/**
	 * Configures whether the value configured in {@link #value()} shall be considered the
	 * upper exclusive or lower inclusive boundary. Defaults to
	 * {@link Range#EQUAL_OR_NEWER}.
	 * 表明是大于等于配置的JavaVersion还是小于配置的JavaVersion
	 *
	 * @return the range
	 */
	Range range() default Range.EQUAL_OR_NEWER;

	/**
	 * The {@link JavaVersion} to check for. Use {@link #range()} to specify whether the
	 * configured value is an upper-exclusive or lower-inclusive boundary.
	 * 配置要检查的java版本.使用range属性来表明大小关系
	 * @return the java version
	 */
	JavaVersion value();

	/**
	 * Range options.
	 */
	enum Range {

		/**
		 * Equal to, or newer than the specified {@link JavaVersion}.
		 * 大于或者等于给定的JavaVersion
		 */
		EQUAL_OR_NEWER,

		/**
		 * Older than the specified {@link JavaVersion}.
		 * 小于指定的 JavaVersion
		 */
		OLDER_THAN

	}

}
