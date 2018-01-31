package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.List;

import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StringRenderer;

public class JspTagMultiline extends BodyTagSupport {

	private List<String> lines;

	public void setLines(List<String> lines) {
		this.lines = lines;
	}

	@Override
	public int doStartTag() throws JspException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtlm());
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	private static final StringRenderer<String> HTML_ESCAPER = HtmlUtils::htmlEscape;

	private String buildHtlm() {
		return CollectionsUtils.toString(lines, HTML_ESCAPER, "<br/>");
	}
}
