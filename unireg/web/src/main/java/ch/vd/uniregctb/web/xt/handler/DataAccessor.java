package ch.vd.uniregctb.web.xt.handler;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springmodules.xt.ajax.MVCFormDataAccessor;

/**
 * @author Pavel BLANCO
 *
 */
public class DataAccessor extends MVCFormDataAccessor {

	@SuppressWarnings("unchecked")
	@Override
	public Object getCommandObject(HttpServletRequest request, HttpServletResponse response, Object handler, Map model) {
		if (handler instanceof BaseCommandController) {
			return super.getCommandObject(request, response, handler, model);
		}
		return null;
	}
}
