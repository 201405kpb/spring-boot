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

import javax.naming.InitialContext;
import java.lang.annotation.*;

/**
 * {@link Conditional @Conditional} that matches based on the availability of a JNDI
 * {@link InitialContext} and the ability to lookup specific locations.
 *
 * JNDI存在指定项
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJndiCondition.class)
public @interface ConditionalOnJndi {

	/**
	 * JNDI Locations, one of which must exist. If no locations are specific the condition
	 * matches solely based on the presence of an {@link InitialContext}.
	 * @return the JNDI locations
	 */
	String[] value() default {};

}
