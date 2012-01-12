package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

public class JspTagPercentIndicator extends BodyTagSupport {

	private static final long serialVersionUID = 2081101363909488271L;

	/**
	 * Largeur totale (en pixels) de la barre de progression, défaut à 100px
	 */
	private int width = 100;

	/**
	 * Indication du pourcentage d'avancement (0-100)
	 */
	private int percent;

	public void setWidth(int width) {
		this.width = width;
	}

	public void setPercent(int percent) {
		if (percent < 0) {
			percent = 0;
		}
		else if (percent > 100) {
			percent = 100;
		}
		this.percent = percent;
	}

	@Override
	public int doStartTag() throws JspException {
		final String rendering = render(width, percent);
		try {
			pageContext.getOut().print(rendering);
		}
		catch (IOException e) {
			throw new JspTagException(e);
		}
		return SKIP_BODY;
	}

	public static String render(int width, int percent) {
		final int pixels = (width * percent) / 100;
		final StringBuilder s = new StringBuilder();
		s.append("<div class=\"progress-bar\" style=\"width: ").append(width).append("px\">");
		s.append("<div class=\"progress-bar-fill\" style=\"width: ").append(pixels).append("px\"></div>");
		s.append("<div class=\"progress-bar-text\" style=\"width: ").append(width).append("px\">").append(percent).append("%</div></div>");
		return s.toString();
	}
}
