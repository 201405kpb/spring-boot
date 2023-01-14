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

package org.springframework.boot.autoconfigure.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;

/**
 * Configuration properties to configure {@link SpringLiquibase}.
 * 配置属性以配置SpringLiquibase。
 *
 * @author Marcel Overdijk
 * @author Eddú Meléndez
 * @author Ferenc Gratzer
 * @author Evgeniy Cheban
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.liquibase", ignoreUnknownFields = false)
public class LiquibaseProperties {

	/**
	 * Change log configuration path.
	 * 更改日志配置路径。
	 */
	private String changeLog = "classpath:/db/changelog/db.changelog-master.yaml";

	/**
	 * Whether to clear all checksums in the current changelog, so they will be
	 * recalculated upon the next update.
	 * 是否清除当前更改日志中的所有校验，以便在下次更新时重新计算。
	 */
	private boolean clearChecksums;

	/**
	 * Comma-separated list of runtime contexts to use.
	 * 要使用的运行时上下文的逗号分隔列表。
	 */
	private String contexts;

	/**
	 * Default database schema.
	 * 默认数据库架构
	 */
	private String defaultSchema;

	/**
	 * Schema to use for Liquibase objects.
	 * 用于Liquibase对象的架构
	 */
	private String liquibaseSchema;

	/**
	 * Tablespace to use for Liquibase objects.
	 * 用于Liquibase对象的表空间
	 */
	private String liquibaseTablespace;

	/**
	 * Name of table to use for tracking change history.
	 * 用于跟踪更改历史记录的表的名称。
	 */
	private String databaseChangeLogTable = "DATABASECHANGELOG";

	/**
	 * Name of table to use for tracking concurrent Liquibase usage.
	 * 用于跟踪并发Liquibase使用情况的表的名称。
	 */
	private String databaseChangeLogLockTable = "DATABASECHANGELOGLOCK";

	/**
	 * Whether to first drop the database schema.
	 * 是否首先删除数据库架构。
	 */
	private boolean dropFirst;

	/**
	 * Whether to enable Liquibase support.
	 * 是否启用Liquibase支持。
	 */
	private boolean enabled = true;

	/**
	 * Login user of the database to migrate.
	 * 要迁移的数据库的登录用户。
	 */
	private String user;

	/**
	 * Login password of the database to migrate.
	 * 要迁移的数据库的登录密码。
	 */
	private String password;

	/**
	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
	 * JDBC驱动程序的完全限定名称。默认情况下，根据URL自动检测。
	 */
	private String driverClassName;

	/**
	 * JDBC URL of the database to migrate. If not set, the primary configured data source is used.
	 * 要迁移的数据库的JDBC URL。如果未设置，则使用主配置数据源。
	 */
	private String url;

	/**
	 * Comma-separated list of runtime labels to use.
	 * 要使用的运行时标签的逗号分隔列表。
	 */
	private String labelFilter;

	/**
	 * Change log parameters.
	 * 更改日志参数
	 */
	private Map<String, String> parameters;

	/**
	 * File to which rollback SQL is written when an update is performed.
	 * 执行更新时将回滚SQL写入的文件。
	 */
	private File rollbackFile;

	/**
	 * Whether rollback should be tested before update is performed.
	 * 是否应在执行更新之前测试回滚。
	 */
	private boolean testRollbackOnUpdate;

	/**
	 * Tag name to use when applying database changes. Can also be used with
	 * "rollbackFile" to generate a rollback script for all existing changes associated
	 * with that tag.
	 * 应用数据库更改时要使用的标记名。还可以与“rollbackFile”一起使用，为与该标记关联的所有现有更改生成回滚脚本。
	 */
	private String tag;

	public String getChangeLog() {
		return this.changeLog;
	}

	public void setChangeLog(String changeLog) {
		Assert.notNull(changeLog, "ChangeLog must not be null");
		this.changeLog = changeLog;
	}

	public String getContexts() {
		return this.contexts;
	}

	public void setContexts(String contexts) {
		this.contexts = contexts;
	}

	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public String getLiquibaseSchema() {
		return this.liquibaseSchema;
	}

	public void setLiquibaseSchema(String liquibaseSchema) {
		this.liquibaseSchema = liquibaseSchema;
	}

	public String getLiquibaseTablespace() {
		return this.liquibaseTablespace;
	}

	public void setLiquibaseTablespace(String liquibaseTablespace) {
		this.liquibaseTablespace = liquibaseTablespace;
	}

	public String getDatabaseChangeLogTable() {
		return this.databaseChangeLogTable;
	}

	public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
		this.databaseChangeLogTable = databaseChangeLogTable;
	}

	public String getDatabaseChangeLogLockTable() {
		return this.databaseChangeLogLockTable;
	}

	public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
		this.databaseChangeLogLockTable = databaseChangeLogLockTable;
	}

	public boolean isDropFirst() {
		return this.dropFirst;
	}

	public void setDropFirst(boolean dropFirst) {
		this.dropFirst = dropFirst;
	}

	public boolean isClearChecksums() {
		return this.clearChecksums;
	}

	public void setClearChecksums(boolean clearChecksums) {
		this.clearChecksums = clearChecksums;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLabelFilter() {
		return this.labelFilter;
	}

	public void setLabelFilter(String labelFilter) {
		this.labelFilter = labelFilter;
	}

	@Deprecated(since = "3.0.0", forRemoval = true)
	@DeprecatedConfigurationProperty(replacement = "spring.liquibase.label-filter")
	public String getLabels() {
		return getLabelFilter();
	}

	@Deprecated(since = "3.0.0", forRemoval = true)
	public void setLabels(String labels) {
		setLabelFilter(labels);
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public File getRollbackFile() {
		return this.rollbackFile;
	}

	public void setRollbackFile(File rollbackFile) {
		this.rollbackFile = rollbackFile;
	}

	public boolean isTestRollbackOnUpdate() {
		return this.testRollbackOnUpdate;
	}

	public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) {
		this.testRollbackOnUpdate = testRollbackOnUpdate;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
