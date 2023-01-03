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

package org.springframework.boot.sql.init;

/**
 * Supported database initialization modes.
 * 支持的数据库初始化模式
 *
 * @author Andy Wilkinson
 * @see AbstractScriptDatabaseInitializer
 * @since 2.5.1
 */
public enum DatabaseInitializationMode {

	/**
	 * Always initialize the database.
	 * 初始化全部数据库
	 */
	ALWAYS,

	/**
	 * Only initialize an embedded database.
	 * 初始化嵌入资源数据库
	 */
	EMBEDDED,

	/**
	 * Never initialize the database.
	 * 从不初始化数据库
	 */
	NEVER

}
