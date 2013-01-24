package ch.vd.uniregctb.servlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

public class UniregAnnotationMethodHandlerAdapter extends AnnotationMethodHandlerAdapter {

	@Override
	protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object target, String objectName) throws Exception {
		return new BooleanAwareDataBinder(target, objectName);
	}
}
