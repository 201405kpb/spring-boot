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

package org.springframework.boot.sql.init.dependency;

import org.springframework.context.annotation.Bean;

import java.lang.annotation.*;

/**
 * Indicate that a bean's creation and initialization depends upon database initialization
 * having completed. May be used on a bean's class or its {@link Bean @Bean} definition.
 * <p>
 * 指示bean的创建和初始化取决于数据库初始化是否完成。可以在bean的类或其｛@linkBean@bean｝定义上使用。
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOnDatabaseInitialization {

}
