package ch.vd.unireg.servlet;

import java.util.List;

import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

/**
 * Request mapping spécialisé pour gérer correctement les checkboxes génériques dans SuperGra (voir {@link UniregExtendedServletRequestDataBinder}).
 */
public class UniregRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {
	@Override
	protected ServletRequestDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods) throws Exception {
		return new UniregServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
	}
}
