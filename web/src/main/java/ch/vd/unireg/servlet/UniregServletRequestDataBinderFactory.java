package ch.vd.unireg.servlet;

import java.util.List;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

/**
 * Factory de request mapping spécialisé pour gérer correctement les checkboxes génériques dans SuperGra (voir {@link UniregExtendedServletRequestDataBinder}).
 */
public class UniregServletRequestDataBinderFactory extends ServletRequestDataBinderFactory {
	/**
	 * Create a new instance.
	 *
	 * @param binderMethods {@code @InitBinder} methods, or {@code null}
	 * @param initializer   for global data binder intialization
	 */
	public UniregServletRequestDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
		super(binderMethods, initializer);
	}

	@Override
	protected ServletRequestDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest request) {
		return new UniregExtendedServletRequestDataBinder(target, objectName);
	}
}
