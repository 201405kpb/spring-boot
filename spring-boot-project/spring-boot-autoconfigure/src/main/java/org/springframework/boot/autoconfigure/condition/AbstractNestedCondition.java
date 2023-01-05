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

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for nested conditions.
 * 嵌套条件的抽象基类，用来组合多个condition
 *
 * @author Phillip Webb
 * @since 1.5.22
 */
public abstract class AbstractNestedCondition extends SpringBootCondition implements ConfigurationCondition {

	private final ConfigurationPhase configurationPhase;

	AbstractNestedCondition(ConfigurationPhase configurationPhase) {
		Assert.notNull(configurationPhase, "ConfigurationPhase must not be null");
		this.configurationPhase = configurationPhase;
	}

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return this.configurationPhase;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String className = getClass().getName();
		//构建成员条件
		MemberConditions memberConditions = new MemberConditions(context, this.configurationPhase, className);
		// 构建成员结果
		MemberMatchOutcomes memberOutcomes = new MemberMatchOutcomes(memberConditions);
		// 调用抽象方法，获取最终的匹配成果
		return getFinalMatchOutcome(memberOutcomes);
	}

	/**
	 * 抽象方法，获取最终的匹配成果
	 *
	 * @param memberOutcomes 成员匹配成果
	 * @return
	 */
	protected abstract ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes);

	/**
	 * 成员匹配结果
	 */
	protected static class MemberMatchOutcomes {

		/**
		 * 全部匹配结果
		 */
		private final List<ConditionOutcome> all;

		/**
		 * 成功匹配结果
		 */
		private final List<ConditionOutcome> matches;

		/**
		 * 失败匹配结果
		 */
		private final List<ConditionOutcome> nonMatches;

		public MemberMatchOutcomes(MemberConditions memberConditions) {
			this.all = memberConditions.getMatchOutcomes();
			List<ConditionOutcome> matches = new ArrayList<>();
			List<ConditionOutcome> nonMatches = new ArrayList<>();
			for (ConditionOutcome outcome : this.all) {
				(outcome.isMatch() ? matches : nonMatches).add(outcome);
			}
			this.matches = Collections.unmodifiableList(matches);
			this.nonMatches = Collections.unmodifiableList(nonMatches);
		}

		public List<ConditionOutcome> getAll() {
			return this.all;
		}

		public List<ConditionOutcome> getMatches() {
			return this.matches;
		}

		public List<ConditionOutcome> getNonMatches() {
			return this.nonMatches;
		}

	}

	/**
	 * 成员条件
	 */
	private static class MemberConditions {

		private final ConditionContext context;

		private final MetadataReaderFactory readerFactory;

		private final Map<AnnotationMetadata, List<Condition>> memberConditions;

		MemberConditions(ConditionContext context, ConfigurationPhase phase, String className) {
			this.context = context;
			this.readerFactory = new SimpleMetadataReaderFactory(context.getResourceLoader());
			//获取成员类
			String[] members = getMetadata(className).getMemberClassNames();
			this.memberConditions = getMemberConditions(members, phase, className);
		}

		/**
		 * 获取成员类的判断条件
		 *
		 * @param members   成员类名称
		 * @param phase     评估阶段
		 * @param className 判断类的名称
		 * @return
		 */
		private Map<AnnotationMetadata, List<Condition>> getMemberConditions(String[] members, ConfigurationPhase phase,
																			 String className) {
			MultiValueMap<AnnotationMetadata, Condition> memberConditions = new LinkedMultiValueMap<>();
			for (String member : members) {
				AnnotationMetadata metadata = getMetadata(member);
				for (String[] conditionClasses : getConditionClasses(metadata)) {
					for (String conditionClass : conditionClasses) {
						Condition condition = getCondition(conditionClass);
						validateMemberCondition(condition, phase, className);
						memberConditions.add(metadata, condition);
					}
				}
			}
			return Collections.unmodifiableMap(memberConditions);
		}

		/**
		 * 验证内部类的执行阶段
		 *
		 * @param condition       判断条件
		 * @param nestedPhase     执行阶段
		 * @param nestedClassName 内部类名称
		 */
		private void validateMemberCondition(Condition condition, ConfigurationPhase nestedPhase,
											 String nestedClassName) {
			if (nestedPhase == ConfigurationPhase.PARSE_CONFIGURATION
					&& condition instanceof ConfigurationCondition configurationCondition) {
				ConfigurationPhase memberPhase = configurationCondition.getConfigurationPhase();
				if (memberPhase == ConfigurationPhase.REGISTER_BEAN) {
					throw new IllegalStateException("Nested condition " + nestedClassName + " uses a configuration "
							+ "phase that is inappropriate for " + condition.getClass());
				}
			}
		}

		/**
		 * 获取元数据
		 *
		 * @param className 类名称
		 * @return
		 */
		private AnnotationMetadata getMetadata(String className) {
			try {
				return this.readerFactory.getMetadataReader(className).getAnnotationMetadata();
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * 获取类的判断条件
		 *
		 * @param metadata 元数据
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
			MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(),
					true);
			Object values = (attributes != null) ? attributes.get("value") : null;
			return (List<String[]>) ((values != null) ? values : Collections.emptyList());
		}

		/**
		 * 获取判断条件实例对象
		 *
		 * @param conditionClassName 判断条件类型名称
		 * @return
		 */
		private Condition getCondition(String conditionClassName) {
			Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName, this.context.getClassLoader());
			return (Condition) BeanUtils.instantiateClass(conditionClass);
		}

		/**
		 * 获取类的匹配结果
		 */
		List<ConditionOutcome> getMatchOutcomes() {
			List<ConditionOutcome> outcomes = new ArrayList<>();
			this.memberConditions.forEach((metadata, conditions) -> outcomes
					.add(new MemberOutcomes(this.context, metadata, conditions).getUltimateOutcome()));
			return Collections.unmodifiableList(outcomes);
		}

	}

	/**
	 * 成员成果
	 */
	private static class MemberOutcomes {

		private final ConditionContext context;

		private final AnnotationMetadata metadata;

		private final List<ConditionOutcome> outcomes;

		MemberOutcomes(ConditionContext context, AnnotationMetadata metadata, List<Condition> conditions) {
			this.context = context;
			this.metadata = metadata;
			this.outcomes = new ArrayList<>(conditions.size());
			for (Condition condition : conditions) {
				this.outcomes.add(getConditionOutcome(metadata, condition));
			}
		}

		private ConditionOutcome getConditionOutcome(AnnotationMetadata metadata, Condition condition) {
			//若 condition 为 SpringBootCondition 类型，则直接执行getMatchOutcome 方法
			if (condition instanceof SpringBootCondition springBootCondition) {
				return springBootCondition.getMatchOutcome(this.context, metadata);
			}
			// 构建 ConditionOutcome 对象
			return new ConditionOutcome(condition.matches(this.context, metadata), ConditionMessage.empty());
		}

		/**
		 * 获取最终的结果
		 *
		 * @return
		 */
		ConditionOutcome getUltimateOutcome() {

			ConditionMessage.Builder message = ConditionMessage
					.forCondition("NestedCondition on " + ClassUtils.getShortName(this.metadata.getClassName()));
			if (this.outcomes.size() == 1) {
				ConditionOutcome outcome = this.outcomes.get(0);
				return new ConditionOutcome(outcome.isMatch(), message.because(outcome.getMessage()));
			}
			List<ConditionOutcome> match = new ArrayList<>();
			List<ConditionOutcome> nonMatch = new ArrayList<>();
			for (ConditionOutcome outcome : this.outcomes) {
				(outcome.isMatch() ? match : nonMatch).add(outcome);
			}
			if (nonMatch.isEmpty()) {
				return ConditionOutcome.match(message.found("matching nested conditions").items(match));
			}
			return ConditionOutcome.noMatch(message.found("non-matching nested conditions").items(nonMatch));
		}

	}

}
