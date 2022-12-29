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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Records condition evaluation details for reporting and logging.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.0.0
 */
public final class ConditionEvaluationReport {

	//评估条件报告类在容器中的bean名称
	private static final String BEAN_NAME = "autoConfigurationReport";
	//创建一个父的条件匹配对象
	private static final AncestorsMatchedCondition ANCESTOR_CONDITION = new AncestorsMatchedCondition();
	//存放类名或方法名（key）,条件评估输出对象（value）
	private final SortedMap<String, ConditionAndOutcomes> outcomes = new TreeMap<>();
	//记录已经从条件评估中排除的类名称
	private final List<String> exclusions = new ArrayList<>();
	//记录作为条件评估的候选类名称
	private final Set<String> unconditionalClasses = new HashSet<>();
	//是否是原始条件匹配对象
	private boolean addedAncestorOutcomes;
	//父的条件评估报告对象
	private ConditionEvaluationReport parent;

	/**
	 * Private constructor.
	 * 提供一个私有的无参构造函数
	 *
	 * @see #get(ConfigurableListableBeanFactory)
	 */
	private ConditionEvaluationReport() {
	}

	/**
	 * Attempt to find the {@link ConditionEvaluationReport} for the specified bean
	 * factory.
	 * 从容器对象中获取条件评估报告对象
	 * @param beanFactory the bean factory (may be {@code null})
	 * @return the {@link ConditionEvaluationReport} or {@code null}
	 */
	public static ConditionEvaluationReport find(BeanFactory beanFactory) {
		if (beanFactory != null && beanFactory instanceof ConfigurableListableBeanFactory) {
			return ConditionEvaluationReport.get((ConfigurableListableBeanFactory) beanFactory);
		}
		return null;
	}

	/**
	 * Obtain a {@link ConditionEvaluationReport} for the specified bean factory.
	 * 从容器中获取ConditionEvaluationReport对象，如果不存在，则创建并注入到IOC容器中
	 * @param beanFactory the bean factory
	 * @return an existing or new {@link ConditionEvaluationReport}
	 */
	public static ConditionEvaluationReport get(ConfigurableListableBeanFactory beanFactory) {
		synchronized (beanFactory) {
			ConditionEvaluationReport report;
			//判定容器中是否存在条件评估报告类对象，如果存在，则取出来
			if (beanFactory.containsSingleton(BEAN_NAME)) {
				report = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
			} else {
				//如果容器中不存在，则创建并注入到IOC容器之中
				report = new ConditionEvaluationReport();
				beanFactory.registerSingleton(BEAN_NAME, report);
			}
			//设置当前条件评估报告对象父对象
			locateParent(beanFactory.getParentBeanFactory(), report);
			return report;
		}
	}

	//设置当前条件评估报告的父对象
	private static void locateParent(BeanFactory beanFactory, ConditionEvaluationReport report) {
		if (beanFactory != null && report.parent == null && beanFactory.containsBean(BEAN_NAME)) {
			report.parent = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
		}
	}

	/**
	 * Record the occurrence of condition evaluation.
	 * 记录条件评估的发生信息
	 * @param source the source of the condition (class or method name) 类名或者方法名
	 * @param condition the condition evaluated 评估条件
	 * @param outcome the condition outcome 评估条件输出
	 */
	public void recordConditionEvaluation(String source, Condition condition, ConditionOutcome outcome) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(condition, "Condition must not be null");
		Assert.notNull(outcome, "Outcome must not be null");
		//删除作为条件评估的候选类名称
		this.unconditionalClasses.remove(source);
		if (!this.outcomes.containsKey(source)) {
			//将类名或方法名（key）及条件评估输出存入集合
			this.outcomes.put(source, new ConditionAndOutcomes());
		}
		//设置条件评估对象及输出对象存入集合
		this.outcomes.get(source).add(condition, outcome);
		//设置addedAncestorOutcomes为false，即：匹配条件不是原始匹配对象
		this.addedAncestorOutcomes = false;
	}

	/**
	 * Records the names of the classes that have been excluded from condition evaluation.
	 * 记录已经从条件评估中排除的类名称
	 * @param exclusions the names of the excluded classes
	 */
	public void recordExclusions(Collection<String> exclusions) {
		Assert.notNull(exclusions, "exclusions must not be null");
		this.exclusions.addAll(exclusions);
	}

	/**
	 * Records the names of the classes that are candidates for condition evaluation.
	 * 记录作为条件评估的候选类名称
	 * @param evaluationCandidates the names of the classes whose conditions will be
	 * evaluated
	 */
	public void recordEvaluationCandidates(List<String> evaluationCandidates) {
		Assert.notNull(evaluationCandidates, "evaluationCandidates must not be null");
		this.unconditionalClasses.addAll(evaluationCandidates);
	}

	/**
	 * Returns condition outcomes from this report, grouped by the source.
	 * 返回匹配的结果集
	 * @return the condition outcomes
	 */
	public Map<String, ConditionAndOutcomes> getConditionAndOutcomesBySource() {
		if (!this.addedAncestorOutcomes) {
			this.outcomes.forEach((source, sourceOutcomes) -> {
				if (!sourceOutcomes.isFullMatch()) {
					addNoMatchOutcomeToAncestors(source);
				}
			});
			this.addedAncestorOutcomes = true;
		}
		return Collections.unmodifiableMap(this.outcomes);
	}

	//将未匹配的输出结果替换为默认的未匹配输出对象
	private void addNoMatchOutcomeToAncestors(String source) {
		String prefix = source + "$";
		this.outcomes.forEach((candidateSource, sourceOutcomes) -> {
			if (candidateSource.startsWith(prefix)) {
				ConditionOutcome outcome = ConditionOutcome
					.noMatch(ConditionMessage.forCondition("Ancestor " + source).because("did not match"));
				sourceOutcomes.add(ANCESTOR_CONDITION, outcome);
			}
		});
	}

	/**
	 * Returns the names of the classes that have been excluded from condition evaluation.
	 * 返回从条件评估中已经排除的类的名称集合
	 * @return the names of the excluded classes
	 */
	public List<String> getExclusions() {
		return Collections.unmodifiableList(this.exclusions);
	}

	/**
	 * Returns the names of the classes that were evaluated but were not conditional.
	 * 返回已经经过条件评估但是不符合条件类的结果集
	 * @return the names of the unconditional classes
	 */
	public Set<String> getUnconditionalClasses() {
		Set<String> filtered = new HashSet<>(this.unconditionalClasses);
		filtered.removeAll(this.exclusions);
		return Collections.unmodifiableSet(filtered);
	}

	/**
	 * The parent report (from a parent BeanFactory if there is one).
	 * 返回条件评估对象的父对象
	 * @return the parent report (or null if there isn't one)
	 */
	public ConditionEvaluationReport getParent() {
		return this.parent;
	}

	public ConditionEvaluationReport getDelta(ConditionEvaluationReport previousReport) {
		//新建一个条件评估报告类
		ConditionEvaluationReport delta = new ConditionEvaluationReport();
		this.outcomes.forEach((source, sourceOutcomes) -> {
			//获取指定类的条件匹配及评估结果类
			ConditionAndOutcomes previous = previousReport.outcomes.get(source);
			if (previous == null || previous.isFullMatch() != sourceOutcomes.isFullMatch()) {
				sourceOutcomes.forEach((conditionAndOutcome) -> delta.recordConditionEvaluation(source,
						conditionAndOutcome.getCondition(), conditionAndOutcome.getOutcome()));
			}
		});
		List<String> newExclusions = new ArrayList<>(this.exclusions);
		newExclusions.removeAll(previousReport.getExclusions());
		delta.recordExclusions(newExclusions);
		List<String> newUnconditionalClasses = new ArrayList<>(this.unconditionalClasses);
		newUnconditionalClasses.removeAll(previousReport.unconditionalClasses);
		delta.unconditionalClasses.addAll(newUnconditionalClasses);
		return delta;
	}

	/**
	 * Provides access to a number of {@link ConditionAndOutcome} items.
	 */
	public static class ConditionAndOutcomes implements Iterable<ConditionAndOutcome> {

		//ConditionAndOutcome评估匹配输出对象集合
		private final Set<ConditionAndOutcome> outcomes = new LinkedHashSet<>();

		//向集合中新增一个评估条件匹配及输出结果对象
		public void add(Condition condition, ConditionOutcome outcome) {
			this.outcomes.add(new ConditionAndOutcome(condition, outcome));
		}

		/**
		 * Return {@code true} if all outcomes match.
		 * 判定集合中的所有评估匹配条件是否匹配
		 * @return {@code true} if a full match
		 */
		public boolean isFullMatch() {
			for (ConditionAndOutcome conditionAndOutcomes : this) {
				if (!conditionAndOutcomes.getOutcome().isMatch()) {
					return false;
				}
			}
			return true;
		}

		//将ConditionAndOutcome对象转换为迭代器模式
		@Override
		public Iterator<ConditionAndOutcome> iterator() {
			return Collections.unmodifiableSet(this.outcomes).iterator();
		}

	}

	/**
	 * Provides access to a single {@link Condition} and {@link ConditionOutcome}.
	 */
	public static class ConditionAndOutcome {

		//条件匹配对象
		private final Condition condition;

		//存储评估匹配条件之后的结果及日志信息
		private final ConditionOutcome outcome;

		//评估匹配条件及输出结果
		public ConditionAndOutcome(Condition condition, ConditionOutcome outcome) {
			this.condition = condition;
			this.outcome = outcome;
		}

		//获取评估匹配判定对象
		public Condition getCondition() {
			return this.condition;
		}

		//获取评估匹配输出结果
		public ConditionOutcome getOutcome() {
			return this.outcome;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ConditionAndOutcome other = (ConditionAndOutcome) obj;
			return (ObjectUtils.nullSafeEquals(this.condition.getClass(), other.condition.getClass())
					&& ObjectUtils.nullSafeEquals(this.outcome, other.outcome));
		}

		@Override
		public int hashCode() {
			return this.condition.getClass().hashCode() * 31 + this.outcome.hashCode();
		}

		@Override
		public String toString() {
			return this.condition.getClass() + " " + this.outcome;
		}

	}

	private static class AncestorsMatchedCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			throw new UnsupportedOperationException();
		}

	}

}
