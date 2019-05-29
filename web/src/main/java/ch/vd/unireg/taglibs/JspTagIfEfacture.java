package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.web.context.WebApplicationContext;

import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Ce tag permet d'inclure conditionnellement un fragment de code JSP en fonction de si le mode efacture est activé ou pas
 */
public class JspTagIfEfacture extends TagSupport {

	@Override
	public int doStartTag() throws JspTagException {

		final WebApplicationContext context = JspTagHelper.getWebApplicationContext(pageContext);
		final UniregModeHelper uniregModeHelper = context.getBean(UniregModeHelper.class, "uniregModeHelper");

		if (uniregModeHelper.isEfactureEnabled()) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}

}
