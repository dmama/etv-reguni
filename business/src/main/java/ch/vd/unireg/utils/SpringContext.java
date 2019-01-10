package ch.vd.unireg.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ch.vd.unireg.message.MessageHelper;

public class SpringContext implements ApplicationContextAware {

	private static ApplicationContext context;

	public static MessageHelper getMessageHelper() {
		return (MessageHelper) context.getBean("messageHelper");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContext.context = applicationContext;
	}
}
