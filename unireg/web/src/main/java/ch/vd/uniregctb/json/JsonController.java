package ch.vd.uniregctb.json;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

public abstract class JsonController extends ParameterizableViewController {

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final String data = buildJsonResponse(request);
		final ServletOutputStream out = response.getOutputStream();
		out.write(data.getBytes("UTF-8"));
		out.flush();
		out.close();

		return null;
	}

	protected abstract String buildJsonResponse(HttpServletRequest request) throws Exception;
}
