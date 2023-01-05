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
 * {@link Conditional @Conditional} that only matches when the specified resources are on
 * the classpath.
 *
 * 类路径下是否存在指定资源文件
 *
 * @author Dave Syer
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnResourceCondition.class)
public @interface ConditionalOnResource {

	/**
	 * The resources that must be present.
	 * 指定的资源必须存在,否则返回不匹配
	 *
	 * @return the resource paths that must be present.
	 */
	String[] resources() default {};

}
