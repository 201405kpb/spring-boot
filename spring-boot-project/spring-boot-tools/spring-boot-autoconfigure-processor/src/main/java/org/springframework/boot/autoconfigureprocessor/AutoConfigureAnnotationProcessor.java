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

package org.springframework.boot.autoconfigureprocessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

/**
 * Annotation processor to store certain annotations from auto-configuration classes in a
 * property file.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 1.5.0
 */
@SupportedAnnotationTypes({"org.springframework.boot.autoconfigure.condition.ConditionalOnClass",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnBean",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication",
		"org.springframework.boot.autoconfigure.AutoConfigureBefore",
		"org.springframework.boot.autoconfigure.AutoConfigureAfter",
		"org.springframework.boot.autoconfigure.AutoConfigureOrder",
		"org.springframework.boot.autoconfigure.AutoConfiguration"})
public class AutoConfigureAnnotationProcessor extends AbstractProcessor {

	/**
	 * 生成的文件
	 */
	protected static final String PROPERTIES_PATH = "META-INF/spring-autoconfigure-metadata.properties";

	/**
	 * 保存指定注解的简称和注解全称之间的对应关系（不可修改）
	 */
	private final Map<String, String> properties = new TreeMap<>();

	private final List<PropertyGenerator> propertyGenerators;

	public AutoConfigureAnnotationProcessor() {
		this.propertyGenerators = Collections.unmodifiableList(getPropertyGenerators());
	}

	protected List<PropertyGenerator> getPropertyGenerators() {
		List<PropertyGenerator> generators = new ArrayList<>();
		addConditionPropertyGenerators(generators);
		addAutoConfigurePropertyGenerators(generators);
		return generators;
	}

	private void addConditionPropertyGenerators(List<PropertyGenerator> generators) {
		String annotationPackage = "org.springframework.boot.autoconfigure.condition";
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
				.withAnnotation(new OnClassConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
				.withAnnotation(new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
				.withAnnotation(new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
				.withAnnotation(ValueExtractor.allFrom("type")));
	}

	private void addAutoConfigurePropertyGenerators(List<PropertyGenerator> generators) {
		String annotationPackage = "org.springframework.boot.autoconfigure";
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
				.withAnnotation(ValueExtractor.allFrom("value", "name"))
				.withAnnotation("AutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
				.withAnnotation(ValueExtractor.allFrom("value", "name"))
				.withAnnotation("AutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
				.withAnnotation(ValueExtractor.allFrom("value")));
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// 遍历上面的几个 `@Conditional` 注解和几个定义自动配置类顺序的注解，依次进行处理
		for (PropertyGenerator generator : this.propertyGenerators) {
			// 对支持的注解进行处理，也就是找到所有标注了该注解的类，然后解析出该注解的值，保存至 Properties
			// 例如 `类名.注解简称` => `注解中的值(逗号分隔)` 和 `类名` => `空字符串`，将自动配置类的信息已经对应注解的信息都保存起来
			// 避免你每次启动 Spring Boot 应用都要去解析自动配置类上面的注解，是引入 `spring-boot-autoconfigure` 后可以从 `META-INF/spring-autoconfigure-metadata.properties` 文件中直接获取
			// 这么一想，Spring Boot 设计的太棒了，所以你自己写的 Spring Boot Starter 中的自动配置模块也可以引入这个 Spring Boot 提供的插件
			process(roundEnv, generator);
		}
		//  如果处理完成
		if (roundEnv.processingOver()) {
			try {
				// 将 Properties 写入 `META-INF/spring-autoconfigure-metadata.properties` 文件
				writeProperties();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		// 返回false
		return false;
	}

	private void process(RoundEnvironment roundEnv, PropertyGenerator generator) {
		for (String annotationName : generator.getSupportedAnnotations()) {
			TypeElement annotationType = this.processingEnv.getElementUtils().getTypeElement(annotationName);
			if (annotationType != null) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
					processElement(element, generator, annotationName);
				}
			}
		}
	}

	private void processElement(Element element, PropertyGenerator generator, String annotationName) {
		try {
			String qualifiedName = Elements.getQualifiedName(element);
			AnnotationMirror annotation = getAnnotation(element, annotationName);
			if (qualifiedName != null && annotation != null) {
				List<Object> values = getValues(generator, annotationName, annotation);
				generator.applyToProperties(this.properties, qualifiedName, values);
				this.properties.put(qualifiedName, "");
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	private List<Object> getValues(PropertyGenerator generator, String annotationName, AnnotationMirror annotation) {
		ValueExtractor extractor = generator.getValueExtractor(annotationName);
		if (extractor == null) {
			return Collections.emptyList();
		}
		return extractor.getValues(annotation);
	}

	private void writeProperties() throws IOException {
		if (!this.properties.isEmpty()) {
			Filer filer = this.processingEnv.getFiler();
			FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", PROPERTIES_PATH);
			try (Writer writer = new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8)) {
				for (Map.Entry<String, String> entry : this.properties.entrySet()) {
					writer.append(entry.getKey());
					writer.append("=");
					writer.append(entry.getValue());
					writer.append(System.lineSeparator());
				}
			}
		}
	}

	@FunctionalInterface
	interface ValueExtractor {

		List<Object> getValues(AnnotationMirror annotation);

		static ValueExtractor allFrom(String... names) {
			return new NamedValuesExtractor(names);
		}

	}

	private abstract static class AbstractValueExtractor implements ValueExtractor {

		@SuppressWarnings("unchecked")
		protected Stream<Object> extractValues(AnnotationValue annotationValue) {
			if (annotationValue == null) {
				return Stream.empty();
			}
			Object value = annotationValue.getValue();
			if (value instanceof List) {
				return ((List<AnnotationValue>) value).stream()
						.map((annotation) -> extractValue(annotation.getValue()));
			}
			return Stream.of(extractValue(value));
		}

		private Object extractValue(Object value) {
			if (value instanceof DeclaredType declaredType) {
				return Elements.getQualifiedName(declaredType.asElement());
			}
			return value;
		}

	}

	private static class NamedValuesExtractor extends AbstractValueExtractor {

		private final Set<String> names;

		NamedValuesExtractor(String... names) {
			this.names = new HashSet<>(Arrays.asList(names));
		}

		@Override
		public List<Object> getValues(AnnotationMirror annotation) {
			List<Object> result = new ArrayList<>();
			annotation.getElementValues().forEach((key, value) -> {
				if (this.names.contains(key.getSimpleName().toString())) {
					extractValues(value).forEach(result::add);
				}
			});
			return result;
		}

	}

	static class OnBeanConditionValueExtractor extends AbstractValueExtractor {

		@Override
		public List<Object> getValues(AnnotationMirror annotation) {
			Map<String, AnnotationValue> attributes = new LinkedHashMap<>();
			annotation.getElementValues()
					.forEach((key, value) -> attributes.put(key.getSimpleName().toString(), value));
			if (attributes.containsKey("name")) {
				return Collections.emptyList();
			}
			List<Object> result = new ArrayList<>();
			extractValues(attributes.get("value")).forEach(result::add);
			extractValues(attributes.get("type")).forEach(result::add);
			return result;
		}

	}

	static class OnClassConditionValueExtractor extends NamedValuesExtractor {

		OnClassConditionValueExtractor() {
			super("value", "name");
		}

		@Override
		public List<Object> getValues(AnnotationMirror annotation) {
			List<Object> values = super.getValues(annotation);
			values.sort(this::compare);
			return values;
		}

		private int compare(Object o1, Object o2) {
			return Comparator.comparing(this::isSpringClass).thenComparing(String.CASE_INSENSITIVE_ORDER)
					.compare(o1.toString(), o2.toString());
		}

		private boolean isSpringClass(String type) {
			return type.startsWith("org.springframework");
		}

	}

	static final class PropertyGenerator {

		private final String annotationPackage;

		private final String propertyName;

		private final boolean omitEmptyValues;

		private final Map<String, ValueExtractor> valueExtractors;

		private PropertyGenerator(String annotationPackage, String propertyName, boolean omitEmptyValues,
				Map<String, ValueExtractor> valueExtractors) {
			this.annotationPackage = annotationPackage;
			this.propertyName = propertyName;
			this.omitEmptyValues = omitEmptyValues;
			this.valueExtractors = valueExtractors;
		}

		PropertyGenerator withAnnotation(ValueExtractor valueExtractor) {
			return withAnnotation(this.propertyName, valueExtractor);
		}

		PropertyGenerator withAnnotation(String name, ValueExtractor ValueExtractor) {
			Map<String, ValueExtractor> valueExtractors = new LinkedHashMap<>(this.valueExtractors);
			valueExtractors.put(this.annotationPackage + "." + name, ValueExtractor);
			return new PropertyGenerator(this.annotationPackage, this.propertyName, this.omitEmptyValues,
					valueExtractors);
		}

		Set<String> getSupportedAnnotations() {
			return this.valueExtractors.keySet();
		}

		ValueExtractor getValueExtractor(String annotation) {
			return this.valueExtractors.get(annotation);
		}

		void applyToProperties(Map<String, String> properties, String className, List<Object> annotationValues) {
			if (this.omitEmptyValues && annotationValues.isEmpty()) {
				return;
			}
			mergeProperties(properties, className + "." + this.propertyName, toCommaDelimitedString(annotationValues));
		}

		private void mergeProperties(Map<String, String> properties, String key, String value) {
			String existingKey = properties.get(key);
			if (existingKey == null || existingKey.isEmpty()) {
				properties.put(key, value);
			}
			else if (!value.isEmpty()) {
				properties.put(key, existingKey + "," + value);
			}
		}

		private String toCommaDelimitedString(List<Object> list) {
			if (list.isEmpty()) {
				return "";
			}
			StringBuilder result = new StringBuilder();
			for (Object item : list) {
				result.append((result.length() != 0) ? "," : "");
				result.append(item);
			}
			return result.toString();
		}

		static PropertyGenerator of(String annotationPackage, String propertyName) {
			return of(annotationPackage, propertyName, false);
		}

		static PropertyGenerator of(String annotationPackage, String propertyName, boolean omitEmptyValues) {
			return new PropertyGenerator(annotationPackage, propertyName, omitEmptyValues, Collections.emptyMap());
		}

	}

}
