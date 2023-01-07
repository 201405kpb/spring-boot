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

package org.springframework.boot.autoconfigure.cache;

/**
 * Supported cache types (defined in order of precedence).
 * 支持的缓存类型（按照优先级定义）
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.3.0
 */
public enum CacheType {

	/**
	 * Generic caching using 'Cache' beans from the context.
	 * 使用上下文中的Cache Bean进行通用缓存
	 */
	GENERIC,

	/**
	 * JCache (JSR-107) backed caching.
	 *  JCache（JSR-107）支持的缓存
	 */
	JCACHE,

	/**
	 * Hazelcast backed caching.
	 */
	HAZELCAST,

	/**
	 * Couchbase backed caching.
	 * Couchbase支持的缓存
	 */
	COUCHBASE,

	/**
	 * Infinispan backed caching.
	 * Infinispan支持的缓存
	 */
	INFINISPAN,

	/**
	 * Redis backed caching.
	 * Redis支持的缓存
	 */
	REDIS,

	/**
	 * Cache2k backed caching.
	 * Cache2k支持的缓存
	 */
	CACHE2K,

	/**
	 * Caffeine backed caching.
	 * Caffeine 支持的缓存
	 */
	CAFFEINE,

	/**
	 * Simple in-memory caching.
	 * 内存基本的简单缓存
	 */
	SIMPLE,

	/**
	 * No caching.
	 * 不支持缓存
	 */
	NONE

}
