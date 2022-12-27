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

package org.springframework.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Indicates that a class provides configuration that can be automatically applied by
 * Spring Boot. Auto-configuration classes are regular
 * {@link Configuration @Configuration} with the exception that
 * {@literal Configuration#proxyBeanMethods() proxyBeanMethods} is always {@code false}.
 * They are located using {@link ImportCandidates}.
 * 指示类提供可由Spring Boot自动应用的配置。
 * <p>
 * Generally auto-configuration classes are marked as {@link Conditional @Conditional}
 * (most often using {@link ConditionalOnClass @ConditionalOnClass} and
 * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
 *
 * @author Moritz Halbritter
 * @see EnableAutoConfiguration
 * @see AutoConfigureBefore
 * @see AutoConfigureAfter
 * @see Conditional
 * @see ConditionalOnClass
 * @see ConditionalOnMissingBean
 * @since 2.7.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore
@AutoConfigureAfter
public @interface AutoConfiguration {

	/**
	 * Explicitly specify the name of the Spring bean definition associated with the
	 * {@code @AutoConfiguration} class. If left unspecified (the common case), a bean
	 * name will be automatically generated.
	 * 显式指定与AutoConfiguration类关联的Spring bean定义的名称。如果未指定（常见情况），将自动生成bean名称。
	 * <p>
	 * The custom name applies only if the {@code @AutoConfiguration} class is picked up
	 * via component scanning or supplied directly to an
	 * {@link AnnotationConfigApplicationContext}. If the {@code @AutoConfiguration} class
	 * is registered as a traditional XML bean definition, the name/id of the bean element
	 * will take precedence.
	 *
	 * @return the explicit component name, if any (or empty String otherwise)
	 * @see AnnotationBeanNameGenerator
	 */
	@AliasFor(annotation = Configuration.class)
	String value() default "";

	/**
	 * The auto-configure classes that should have not yet been applied.
	 * 尚未应用的自动配置类。
	 * @return the classes
	 */
	@AliasFor(annotation = AutoConfigureBefore.class, attribute = "value")
	Class<?>[] before() default {};

	/**
	 * The names of the auto-configure classes that should have not yet been applied.
	 * 尚未应用的自动配置类的名称
	 * @return the class names
	 */
	@AliasFor(annotation = AutoConfigureBefore.class, attribute = "name")
	String[] beforeName() default {};

	/**
	 * The auto-configure classes that should have already been applied.
	 * 应已应用的自动配置类
	 * @return the classes
	 */
	@AliasFor(annotation = AutoConfigureAfter.class, attribute = "value")
	Class<?>[] after() default {};

	/**
	 * The names of the auto-configure classes that should have already been applied.
	 * 应已应用的自动配置类名称
	 * @return the class names
	 */
	@AliasFor(annotation = AutoConfigureAfter.class, attribute = "name")
	String[] afterName() default {};

}
