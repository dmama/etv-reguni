package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ReadOnlyPropertyDescriptor;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Tag jsp qui permet de récupérer les éléments d'une commune (à une date donnée) depuis l'infrastructure
 */
public class JspTagCommune extends BodyTagSupport {

	private final Logger LOGGER = Logger.getLogger(JspTagCommune.class);

	private Integer ofs;
	private String displayProperty;
	private String titleProperty;
	private RegDate date;
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans le context d'appels jsp

	@Override
	public int doStartTag() throws JspException {

		Object property = null;
		Object title = null;
		try {
			final Commune commune = service.getCommuneByNumeroOfsEtendu(ofs, date);
			if (commune != null) {
				final ReadOnlyPropertyDescriptor displayDescriptor = new ReadOnlyPropertyDescriptor(displayProperty, Commune.class);
				property = displayDescriptor.getReadMethod().invoke(commune);

				if (StringUtils.isNotBlank(titleProperty)) {
					final ReadOnlyPropertyDescriptor titleDescriptor = new ReadOnlyPropertyDescriptor(titleProperty, Commune.class);
					title = titleDescriptor.getReadMethod().invoke(commune);
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new JspException(e);
		}

		if (property != null) {
			try {
				final StringBuilder b = new StringBuilder();
				if (title != null) {
					b.append("<span title='").append(HtmlUtils.htmlEscape(title.toString())).append("'>");
				}
				b.append(HtmlUtils.htmlEscape(property.toString()));
				if (title != null) {
					b.append("</span>");
				}
				pageContext.getOut().print(b.toString());
			}
			catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
				throw new JspException(e);
			}
		}
		return SKIP_BODY;
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

	public void setService(ServiceInfrastructureService service) {
		JspTagCommune.service = service;
	}
}
