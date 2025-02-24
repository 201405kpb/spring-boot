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

package org.springframework.boot.context.properties;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * Annotation for externalized configuration. Add this to a class definition or a
 * {@code @Bean} method in a {@code @Configuration} class if you want to bind and validate
 * some external Properties (e.g. from a .properties file).
 * 外部化配置的注释。
 * <p>
 * Binding is either performed by calling setters on the annotated class or, if
 * {@link ConstructorBinding @ConstructorBinding} is in use, by binding to the constructor
 * parameters.
 * 绑定可以通过在带注释的类上调用setter来执行，或者通过绑定到构造函数参数来执行。
 * <p>
 * Note that contrary to {@code @Value}, SpEL expressions are not evaluated since property
 * values are externalized.
 * 由于属性值是外部化的，因此不会计算SpEL表达式
 *
 * @author Dave Syer
 * @see ConfigurationPropertiesScan
 * @see ConstructorBinding
 * @see ConfigurationPropertiesBindingPostProcessor
 * @see EnableConfigurationProperties
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface ConfigurationProperties {

	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #prefix()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 *
	 * @return the prefix of the properties to bind 要绑定的属性的前缀
	 */
	@AliasFor("prefix")
	String value() default "";

	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #value()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 * @return the prefix of the properties to bind 要绑定的属性的前缀
	 */
	@AliasFor("value")
	String prefix() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * 当属性值绑定到字段，发生错误时，是否忽略异常。默认不忽略，会抛出异常
	 * @return the flag value (default false)
	 */
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * 当配置项向实体类中的属性绑定时，没有找到对应的字段，是否忽略。默认忽略，不抛出异常。
	 * @return the flag value (default true)
	 */
	boolean ignoreUnknownFields() default true;

}
