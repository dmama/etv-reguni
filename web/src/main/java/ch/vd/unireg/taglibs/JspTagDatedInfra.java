package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Classe de base des tags JSP qui permettent d'aller retrouver des éléments datés dans l'infrastrucure
 */
public abstract class JspTagDatedInfra<T> extends BodyTagSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(JspTagDatedInfra.class);

	private final Class<T> clazz;

	private Integer ofs;
	private String displayProperty;
	private String titleProperty;
	private RegDate date;
	private EscapeMode escapeMode = EscapeMode.HTML;
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans le context d'appels jsp

	protected abstract T getInstance(ServiceInfrastructureService infraService, Integer noOfs, RegDate date) throws ServiceInfrastructureException;

	protected JspTagDatedInfra(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public int doStartTag() throws JspException {

		Object property = null;
		Object title = null;
		try {
			final T instance = getInstance(service, ofs, date);
			if (instance != null) {
				property = PropertyUtils.getProperty(instance, displayProperty);

				if (StringUtils.isNotBlank(titleProperty)) {
					title = PropertyUtils.getProperty(instance, titleProperty);
				}
			}

			if (property != null) {
				final StringBuilder b = new StringBuilder();
				if (title != null) {
					b.append("<span title='").append(HtmlUtils.htmlEscape(title.toString())).append("'>");
				}
				b.append(escapeMode.escape(property.toString()));
				if (title != null) {
					b.append("</span>");
				}
				print(b.toString());
			}
		}
		catch (Exception e) {
			// [SIFISC-5427] le mécanisme d'interception des exceptions (voir le bean 'urlMappingExceptionResolver') ne fonctionne pas dans le context
			// des tags JSP, on doit donc directement afficher un message d'erreur pour signaler le problème.
			LOGGER.error(e.getMessage(), e);
			print("<span class=\"error\">##Exception : " + e.getMessage() + "##</span>");
		}
		return SKIP_BODY;
	}

	private void print(String b) throws JspException {
		try {
			pageContext.getOut().print(b);
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new JspException(e);
		}
	}

	public void setOfs(Integer ofs) {
		this.ofs = ofs;
	}

	public void setDisplayProperty(String displayProperty) {
		this.displayProperty = displayProperty;
	}

	public void setTitleProperty(String titleProperty) {
		this.titleProperty = titleProperty;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public void setEscapeMode(String escapeMode) {
		this.escapeMode = EscapeMode.fromString(escapeMode);
		if (this.escapeMode == null) {
			this.escapeMode = EscapeMode.HTML;
		}
	}

	public void setService(ServiceInfrastructureService service) {
		JspTagDatedInfra.service = service;
	}
}
