package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.web.context.WebApplicationContext;

import ch.vd.unireg.utils.UniregModeHelper;

public class JspTagTestMode extends TagSupport {

	private static final long serialVersionUID = 7341126434765629518L;
	
	@Override
	public int doStartTag() throws JspTagException {

		final WebApplicationContext context = JspTagHelper.getWebApplicationContext(pageContext);
		final UniregModeHelper uniregModeHelper = context.getBean(UniregModeHelper.class, "uniregModeHelper");

		if (uniregModeHelper.isTestMode()) {
			return Tag.EVAL_BODY_INCLUDE;
		}
		
		return Tag.SKIP_BODY;
	}
}
