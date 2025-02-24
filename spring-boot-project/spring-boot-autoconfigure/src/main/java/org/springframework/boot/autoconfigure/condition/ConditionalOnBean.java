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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * {@link Conditional @Conditional} that only matches when beans meeting all the specified
 * requirements are already contained in the {@link BeanFactory}. All the requirements
 * must be met for the condition to match, but they do not have to be met by the same
 * bean.
 * 容器中存在指定Bean;
 * <p>
 * When placed on a {@code @Bean} method, the bean class defaults to the return type of
 * the factory method:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAutoConfiguration {
 *
 *     &#064;ConditionalOnBean
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 *
 * }</pre>
 * <p>
 * In the sample above the condition will match if a bean of type {@code MyService} is
 * already contained in the {@link BeanFactory}.
 * <p>
 * The condition can only match the bean definitions that have been processed by the
 * application context so far and, as such, it is strongly recommended to use this
 * condition on auto-configuration classes only. If a candidate bean may be created by
 * another auto-configuration, make sure that the one using this condition runs after.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

	/**
	 * The class types of beans that should be checked. The condition matches when beans
	 * of all classes specified are contained in the {@link BeanFactory}.
	 * bean的类型,当ApplicationContext包含给定类的bean时返回true
	 *
	 * @return the class types of beans to check
	 */
	Class<?>[] value() default {};

	/**
	 * The class type names of beans that should be checked. The condition matches when
	 * beans of all classes specified are contained in the {@link BeanFactory}.
	 * bean的类型名,当ApplicationContext包含给定的id时返回true
	 * @return the class type names of beans to check
	 */
	String[] type() default {};

	/**
	 * The annotation type decorating a bean that should be checked. The condition matches
	 * when all the annotations specified are defined on beans in the {@link BeanFactory}.
	 * bean所声明的注解,当ApplicationContext中存在声明该注解的bean时返回true
	 * @return the class-level annotation types to check
	 */
	Class<? extends Annotation>[] annotation() default {};

	/**
	 * The names of beans to check. The condition matches when all the bean names
	 * specified are contained in the {@link BeanFactory}.
	 * bean的id,,当ApplicationContext中存在给定id的bean时返回true
	 * @return the names of beans to check
	 */
	String[] name() default {};

	/**
	 * Strategy to decide if the application context hierarchy (parent contexts) should be
	 * considered.
	 * 搜索策略，默认是所有上下文搜索
	 * @return the search strategy
	 */
	SearchStrategy search() default SearchStrategy.ALL;

	/**
	 * Additional classes that may contain the specified bean types within their generic
	 * parameters. For example, an annotation declaring {@code value=Name.class} and
	 * {@code parameterizedContainer=NameRegistration.class} would detect both
	 * {@code Name} and {@code NameRegistration<Name>}.
	 * 指定 bean 类的类型：泛型类
	 * 匹配规则：存在某个 bean 为 A ，A 的类型和 parameterizedContainer 指定的泛型类的类型一致（可以是它的实现类或继承子类），
	 * 且 A 的泛型参数是 @ConditionalOnBean 注解的 bean 的类型
	 * @return the container types
	 * @since 2.1.0
	 */
	Class<?>[] parameterizedContainer() default {};

}
