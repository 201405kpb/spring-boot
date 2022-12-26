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

package org.springframework.boot.context.properties;

import java.lang.annotation.*;

/**
 * Indicates that a getter in a {@link ConfigurationProperties @ConfigurationProperties}
 * object is deprecated. This annotation has no bearing on the actual binding processes,
 * but it is used by the {@code spring-boot-configuration-processor} to add deprecation
 * meta-data.
 * 指示ConfigurationProperties对象中的getter已弃用。此注释与实际绑定过程无关，但spring boot配置处理器使用它来添加弃用元数据。
 * <p>
 * This annotation <strong>must</strong> be used on the getter of the deprecated element.
 * 此注释必须用于已弃用元素的getter。
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeprecatedConfigurationProperty {

	/**
	 * The reason for the deprecation.
	 * @return the deprecation reason
	 */
	String reason() default "";

	/**
	 * The field that should be used instead (if any).
	 * @return the replacement field
	 */
	String replacement() default "";

}
