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

package org.springframework.boot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ApplicationArguments}.
 *
 * @author Phillip Webb
 * @since 1.4.1
 */
public class DefaultApplicationArguments implements ApplicationArguments {

	//内部类，获取参数的主要操作都在此类中
	private final Source source;

	//应用程序原始未处理的参数
	private final String[] args;

	//构造函数，args不可以为null
	public DefaultApplicationArguments(String... args) {
		Assert.notNull(args, "Args must not be null");
		this.source = new Source(args);
		this.args = args;
	}

	//获取应用程序原始未处理的参数
	@Override
	public String[] getSourceArgs() {
		return this.args;
	}

	//获取应用程序解析后的所有选项参数名
	@Override
	public Set<String> getOptionNames() {
		String[] names = this.source.getPropertyNames();
		return Set.of(names);
	}

	//判定解析后的选项是否包含指定的参数
	@Override
	public boolean containsOption(String name) {
		return this.source.containsProperty(name);
	}

	//获取所有解析后的选项参数值
	@Override
	public List<String> getOptionValues(String name) {
		List<String> values = this.source.getOptionValues(name);
		return (values != null) ? Collections.unmodifiableList(values) : null;
	}

	//获取所有非选项参数集合，如：[ "foo=bar", "foo=baz", "foo1=biz" ]
	@Override
	public List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

	//内部类，继承了SimpleCommandLinePropertySource对命令行参数进行操作
	private static class Source extends SimpleCommandLinePropertySource {

		Source(String[] args) {
			super(args);
		}

		@Override
		public List<String> getNonOptionArgs() {
			return super.getNonOptionArgs();
		}

		@Override
		public List<String> getOptionValues(String name) {
			return super.getOptionValues(name);
		}

	}

}
