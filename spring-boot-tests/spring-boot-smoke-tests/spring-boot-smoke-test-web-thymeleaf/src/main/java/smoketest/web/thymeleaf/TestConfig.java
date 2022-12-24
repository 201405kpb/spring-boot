package smoketest.web.thymeleaf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class TestConfig {

	@Bean
	public MessageRepository messageRepository() {
		return new InMemoryMessageRepository();
	}

	@Bean
	public Converter<String, Message> messageConverter() {
		return new Converter<String, Message>() {
			@Override
			public Message convert(String id) {
				return messageRepository().findMessage(Long.valueOf(id));
			}
		};
	}
}
