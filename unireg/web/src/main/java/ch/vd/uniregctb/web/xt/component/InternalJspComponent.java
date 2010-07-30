package ch.vd.uniregctb.web.xt.component;

import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang.StringEscapeUtils;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.support.RenderingException;

public class InternalJspComponent implements Component {

	private static final long serialVersionUID = 26L;

	private final HttpServletRequest request;
	private final String path;

	private String characterEncoding;
	private String contentType;
	private Locale locale;

	/**
	 * Construct the component.
	 *
	 * @param request
	 *            The HttpServletRequest to use for dynamic content.
	 * @param path
	 *            The Jsp path.
	 */
	public InternalJspComponent(HttpServletRequest request, String path) {
		this.request = request;
		this.path = path;
	}

	public String render() {
		StringWriter writer = null;
		String str = null;
		try {
			writer = new StringWriter();
			// Corrige bug avec WebLogic
			HttpServletResponse response = new HttpServletResponseWrapper(new InternalHttpServletResponse(writer));
			response.setCharacterEncoding(this.characterEncoding);
			response.setContentType(this.contentType);
			response.setLocale(this.locale);
			String url = this.path;
			int index = url.indexOf(request.getContextPath());
			if ( index > 0 ){
				url= url.substring(index+request.getContextPath().length());
			}
			request.getRequestDispatcher(url).include(this.request, response);
			str = writer.toString();
		}
		catch (ClassCastException cce) {
			throw new RenderingException("There is probably a problem with requested URL: " + this.path, cce);
		}
		catch (Exception e) {
			throw new RenderingException("Error while rendering a component of type: " + this.getClass().getName(), e);
		}
		// xt javascript accepte seulement une xml représentation
		str = StringEscapeUtils.unescapeHtml(str) ;
		//str = StringEscapeUtils.escapeXml(str);
		return str;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}