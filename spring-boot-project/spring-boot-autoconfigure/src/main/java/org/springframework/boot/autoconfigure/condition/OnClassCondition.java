/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Condition} and {@link AutoConfigurationImportFilter} that checks for the
 * presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class OnClassCondition extends FilteringSpringBootCondition {

	/**
	 * 该方法来自 {@link AutoConfigurationImportFilter} 判断这些自动配置类是否符合条件（`@ConditionalOnClass`）
	 */
	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
												   AutoConfigurationMetadata autoConfigurationMetadata) {
		// Split the work and perform half in a background thread if more than one
		// processor is available. Using a single additional thread seems to offer the
		// best performance. More threads make things worse.
		// 考虑到自动配置类上面的 `@Conditional` 相关注解比较多，所以采用多线程以提升效率。经过测试使用，使用两个线程的效率是最高的，
		// 所以会将 `autoConfigurationClasses` 一分为二进行处理

		// <1> 如果 JVM 可用的处理器不止一个，那么这里用两个线程去处理
		if (autoConfigurationClasses.length > 1 && Runtime.getRuntime().availableProcessors() > 1) {
			// <1.1> 对 `autoConfigurationClasses` 所有的自动配置类进行处理
			// 这里是对 `@ConditionalOnClass` 注解进行处理，必须存在指定 Class 类对象
			return resolveOutcomesThreaded(autoConfigurationClasses, autoConfigurationMetadata);
		}
		// <2> 否则，就是单核处理，当前线程去处理
		else {
			// <2.1> 创建一个匹配处理器 `outcomesResolver`
			OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, 0,
					autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
			// <2.2> 返回 `outcomesResolver` 的执行结果
			// 这里是对 `@ConditionalOnClass` 注解进行处理，必须存在指定 Class 类对象
			return outcomesResolver.resolveOutcomes();
		}
	}

	//如果配置类有多个，并且有多个处理器，则选择拆分处理的方法
	private ConditionOutcome[] resolveOutcomesThreaded(String[] autoConfigurationClasses,
													   AutoConfigurationMetadata autoConfigurationMetadata) {
		//获取配置类一半的数量
		int split = autoConfigurationClasses.length / 2;
		//创建线程解析器解析第一部分配置类
		OutcomesResolver firstHalfResolver = createOutcomesResolver(autoConfigurationClasses, 0, split,
				autoConfigurationMetadata);
		//创建标准的解析器解析第二部分配置类
		OutcomesResolver secondHalfResolver = new StandardOutcomesResolver(autoConfigurationClasses, split,
				autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
		//使用标准解析器解析第二部分配置类
		ConditionOutcome[] secondHalf = secondHalfResolver.resolveOutcomes();
		//使用线程解析器解析第一部分配置类
		ConditionOutcome[] firstHalf = firstHalfResolver.resolveOutcomes();
		//创建解析后的结果数据集
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		//将第一部分解析的结果放入数组
		System.arraycopy(firstHalf, 0, outcomes, 0, firstHalf.length);
		//将第二部分解析的结果放入数组
		System.arraycopy(secondHalf, 0, outcomes, split, secondHalf.length);
		return outcomes;
	}

	private OutcomesResolver createOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
													AutoConfigurationMetadata autoConfigurationMetadata) {
		// 创建一个匹配处理器
		OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, start, end,
				autoConfigurationMetadata, getBeanClassLoader());
		// 将上面的处理器封装到一个线程里面进行处理
		return new ThreadedOutcomesResolver(outcomesResolver);
	}

	/**
	 * 该方法来自 {@link SpringBootCondition} 判断某个 Bean 是否符合注入条件（`@ConditionalOnClass` 和 `ConditionalOnMissingClass`）
	 */
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ClassLoader classLoader = context.getClassLoader();
		ConditionMessage matchMessage = ConditionMessage.empty();
		// <1> 获取这个类上面的 `@ConditionalOnClass` 注解的值
		// 也就是哪些 Class 对象必须存在
		List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
		if (onClasses != null) {
			// <1.1> 找到这些 Class 对象中哪些是不存在的
			List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
			// <1.2> 如果存在不存在的，那么不符合条件，返回不匹配
			if (!missing.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
					.didNotFind("required class", "required classes")
					.items(Style.QUOTE, missing));
			}
			// <1.3> 添加 `@ConditionalOnClass` 满足条件的匹配信息
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
				.found("required class", "required classes")
				.items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
		}
		// <2> 获取这个类上面的 `@ConditionalOnMissingClass` 注解的值，也就是哪些 Class 对象不存在
		List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			// <2.1> 找到这些 Class 对象中哪些是存在的
			List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
			// <2.2> 如果存在存在的，那么不符合条件，返回不匹配
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
					.found("unwanted class", "unwanted classes")
					.items(Style.QUOTE, present));
			}
			// <1.3> 添加 `@ConditionalOnMissingClass` 满足条件的匹配信息
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
				.didNotFind("unwanted class", "unwanted classes")
				.items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
		}
		return ConditionOutcome.match(matchMessage);
	}

	private List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
		if (attributes == null) {
			return null;
		}
		List<String> candidates = new ArrayList<>();
		addAll(candidates, attributes.get("value"));
		addAll(candidates, attributes.get("name"));
		return candidates;
	}

	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private interface OutcomesResolver {

		ConditionOutcome[] resolveOutcomes();

	}

	private static final class ThreadedOutcomesResolver implements OutcomesResolver {

		//线程对象
		private final Thread thread;

		//配置类注解解析后的结果集
		private volatile ConditionOutcome[] outcomes;

		//根据给定的解析器构建ThreadedOutcomesResolver实例对象
		private ThreadedOutcomesResolver(OutcomesResolver outcomesResolver) {
			//使用异步线程的模式在后台解析给定的配置类
			this.thread = new Thread(() -> this.outcomes = outcomesResolver.resolveOutcomes());
			this.thread.start();
		}

		@Override
		public ConditionOutcome[] resolveOutcomes() {
			try {
				this.thread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return this.outcomes;
		}

	}

	private static final class StandardOutcomesResolver implements OutcomesResolver {

		/**
		 * 需要处理的自动配置类
		 */
		private final String[] autoConfigurationClasses;

		/**
		 * 区间开始位置
		 */
		private final int start;

		/**
		 * 区间结束位置
		 */
		private final int end;

		/**
		 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} 注解的元信息
		 */
		private final AutoConfigurationMetadata autoConfigurationMetadata;

		/**
		 * 类加载器
		 */
		private final ClassLoader beanClassLoader;

		/**
		 * 新建一个标准的条件匹配解析实例对象
		 *
		 * @param autoConfigurationClasses
		 * @param start
		 * @param end
		 * @param autoConfigurationMetadata
		 * @param beanClassLoader
		 */
		private StandardOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
										 AutoConfigurationMetadata autoConfigurationMetadata, ClassLoader beanClassLoader) {
			this.autoConfigurationClasses = autoConfigurationClasses;
			this.start = start;
			this.end = end;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
			this.beanClassLoader = beanClassLoader;
		}

		/**
		 * @return 解析配置类是否符合条件并返回结果集
		 */
		@Override
		public ConditionOutcome[] resolveOutcomes() {
			return getOutcomes(this.autoConfigurationClasses, this.start, this.end, this.autoConfigurationMetadata);
		}

		private ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses, int start, int end,
											   AutoConfigurationMetadata autoConfigurationMetadata) {
			ConditionOutcome[] outcomes = new ConditionOutcome[end - start];
			/**
			 * 遍历执行区间内的自动配置类
			 */
			for (int i = start; i < end; i++) {
				String autoConfigurationClass = autoConfigurationClasses[i];
				if (autoConfigurationClass != null) {
					/**
					 * 获取这个自动配置类的 `@ConditionalOnClass` 注解的值
					 * 这里不需要解析，而是从一个 `Properties` 中直接获取
					 * 参考 {@link AutoConfigurationImportSelector} 中我的注释
					 * 参考 {@link AutoConfigureAnnotationProcessor}
					 */
					String candidates = autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnClass");
					// 如果值不为空，那么这里先进行匹配处理，判断这个自动配置类是否需要注入，也就是是否存在 指定的 Class 对象（`candidates`）
					// 否则，不进行任何处理，也就是不过滤掉
					if (candidates != null) {
						// 判断指定的 Class 类对象是否都存在，都存在返回 `null`，有一个不存在返回不匹配
						outcomes[i - start] = getOutcome(candidates);
					}
				}
			}
			return outcomes;
		}

		/**
		 * 解析条件类是否存在，如果不存在返回ConditionOutcome对象match为false及日志信息
		 * 如果符合条件，即类实例存在，则返回null
		 *
		 * @param candidates
		 * @return
		 */
		private ConditionOutcome getOutcome(String candidates) {
			try {
				// 这个配置类的 `@ConditionalOnClass` 注解只指定了一个，则直接处理
				if (!candidates.contains(",")) {
					// 判断这个 Class 类对象是否存在，存在返回 `null`，不存在返回不匹配
					return getOutcome(candidates, this.beanClassLoader);
				}
				// 这个配置类的 `@ConditionalOnClass` 注解指定了多个，则遍历处理，必须都存在
				for (String candidate : StringUtils.commaDelimitedListToStringArray(candidates)) {
					// 判断这个 Class 类对象是否存在，存在返回 `null`，不存在返回不匹配
					ConditionOutcome outcome = getOutcome(candidate, this.beanClassLoader);
					// 如果不为空，表示不匹配，直接返回
					if (outcome != null) {
						return outcome;
					}
				}
			} catch (Exception ex) {
				// We'll get another chance later
			}
			return null;
		}

		/**
		 * 获取条件类验证的结果
		 *
		 * @param className
		 * @param classLoader
		 * @return
		 */
		private ConditionOutcome getOutcome(String className, ClassLoader classLoader) {
			// 如果这个 Class 对象不存在，则返回不匹配
			if (ClassNameFilter.MISSING.matches(className, classLoader)) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
					.didNotFind("required class")
					.items(Style.QUOTE, className));
			}
			//如果条件类存在，则返回null
			return null;
		}

	}

}
