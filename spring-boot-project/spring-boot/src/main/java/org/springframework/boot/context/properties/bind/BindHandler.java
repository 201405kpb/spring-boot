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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Callback interface that can be used to handle additional logic during element
 * {@link Binder binding}.
 * 可用于在元素binding期间处理附加逻辑的 回调接口
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public interface BindHandler {

	/**
	 * Default no-op bind handler.
	 * 默认无操作绑定处理程序
	 */
	BindHandler DEFAULT = new BindHandler() {

	};

	/**
	 * Called when binding of an element starts but before any result has been determined.
	 * 在元素绑定开始和在确定任何结果之前调用。
	 * @param <T> the bindable source type
	 * @param name the name of the element being bound 被绑定的元素名称
	 * @param target the item being bound 正在绑定的项
	 * @param context the bind context 绑定上下文
	 * @return the actual item that should be used for binding (may be {@code null}) 应用该绑定的元素，实际可能是null
	 */
	default <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
		return target;
	}

	/**
	 * Called when binding of an element ends with a successful result. Implementations
	 * may change the ultimately returned result or perform addition validation.
	 * 当元素绑定以成功结果结束时调用。 实现可能会更改最终返回的结果或执行添加验证。
	 * @param name the name of the element being bound 正在绑定元素的名称
	 * @param target the item being bound 正在绑定的元素
	 * @param context the bind context 绑定上下文
	 * @param result the bound result (never {@code null}) 绑定结果，不可能为空
	 * @return the actual result that should be used (may be {@code null}) 应使用的实际结果，可能为空
	 */
	default Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		return result;
	}

	/**
	 * Called when binding of an element ends with an unbound result and a newly created
	 * instance is about to be returned. Implementations may change the ultimately
	 * returned result or perform addition validation.
	 *  当元素的绑定以未绑定的结果结束并且即将返回新创建的实例时调用。 实现可能会更改最终返回的结果或执行添加验证。
	 * @param name the name of the element being bound 正在绑定元素的名称
	 * @param target the item being bound 正在绑定的元素
	 * @param context the bind context 绑定上下文
	 * @param result the newly created instance (never {@code null}) 新创建的实例，不可能为空
	 * @return the actual result that should be used (must not be {@code null})  应使用的实际结果，不可能为空
	 * @since 2.2.2
	 */
	default Object onCreate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		return result;
	}

	/**
	 * Called when binding fails for any reason (including failures from
	 * {@link #onSuccess} or {@link #onCreate} calls). Implementations may choose to
	 * swallow exceptions and return an alternative result.
	 * 当绑定因任何原因失败（包括onSuccess或onCreate调用失败）时调用。 实现可以选择吞下异常并返回一个替代结果。
	 * @param name the name of the element being bound 被绑定元素的名称
	 * @param target the item being bound 正在被绑定的元素
	 * @param context the bind context 绑定上下文
	 * @param error the cause of the error (if the exception stands it may be re-thrown) 错误原因(如果异常成立，则可能会重新抛出)
	 * @return the actual result that should be used (may be {@code null}). 应用使用的实际结果(可能为null)
	 * @throws Exception if the binding isn't valid 绑定失败抛出异常
	 */
	default Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
			throws Exception {
		throw error;
	}

	/**
	 * Called when binding finishes with either bound or unbound result. This method will
	 * not be called when binding failed, even if a handler returns a result from
	 * {@link #onFailure}.
	 * 当绑定以绑定或未绑定结果结束时调用。 绑定失败时不会调用此方法，即使处理程序从onFailure返回结果。
	 * @param name the name of the element being bound 被绑定的元素名称
	 * @param target the item being bound 被绑定的元素
	 * @param context the bind context 绑定上下文
	 * @param result the bound result (may be {@code null}) 绑定结果，可能为null
	 * @throws Exception if the binding isn't valid 绑定失败抛出异常
	 */
	default void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
			throws Exception {
	}

}
