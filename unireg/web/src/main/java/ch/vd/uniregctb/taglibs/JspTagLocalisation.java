package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.util.HtmlUtils;

import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.view.AdresseCivilView;
import ch.vd.uniregctb.tiers.view.LocalisationView;

/**
 * Tag jsp qui permet d'afficher une localisation (provenance ou destination) avec le détails de l'adresse dans un tooltip.
 */
public class JspTagLocalisation extends BodyTagSupport {

	private final Logger LOGGER = Logger.getLogger(JspTagLocalisation.class);

	private final static AtomicInteger counter = new AtomicInteger();

	private LocalisationView localisation;
	private boolean showVD;
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans le context d'appels jsp

	public JspTagLocalisation() {
	}

	@Override
	public int doStartTag() throws JspException {

		final String tooltipId = "localisation-" + counter.incrementAndGet();

		if (localisation != null) {

			try {
				print(HtmlUtils.htmlEscape(getLabel()) + "&nbsp;" + getAdresse(tooltipId));
			}
			catch (Exception e) {
				// [SIFISC-5427] le mécanisme d'interception des exceptions (voir le bean 'urlMappingExceptionResolver') ne fonctionne pas dans le context
				// des tags JSP, on doit donc directement afficher un message d'erreur pour signaler le problème.
				LOGGER.error(e.getMessage(), e);
				print("<span class=\"error\">##Exception : " + e.getMessage() + "##</span>");
			}
		}
		return SKIP_BODY;
	}

	private String getAdresse(String tooltipId) {
		String html = "";
		final AdresseCivilView adresse = localisation.getAdresseCourrier();
		if (adresse != null && (localisation.getType() != LocalisationType.CANTON_VD || showVD)) {

			final StringBuilder sb = new StringBuilder();
			sb.append("<a href=\"#\" class=\"consult staticTip\" id=\"").append(tooltipId).append("\" title=\"Adresse courrier\">&nbsp;</a>");
			sb.append("<div id=\"").append(tooltipId).append("-tooltip\" style=\"display:none;\">");

			if (StringUtils.isNotBlank(adresse.getComplements())) {
				sb.append(HtmlUtils.htmlEscape(adresse.getComplements())).append("<br/>");
			}
			if (StringUtils.isNotBlank(adresse.getRue())) {
				sb.append(HtmlUtils.htmlEscape(adresse.getRue())).append("<br/>");
			}
			if (StringUtils.isNotBlank(adresse.getLocalite())) {
				sb.append(HtmlUtils.htmlEscape(adresse.getLocalite())).append("<br/>");
			}
			if (adresse.getPaysOFS() != null) {
				sb.append(HtmlUtils.htmlEscape(getPays(adresse.getPaysOFS()))).append("<br/>");
			}

			sb.append("</div>");
			html = sb.toString();
		}
		return html;
	}

	private String getLabel() throws JspException {
		switch (localisation.getType()) {
		case CANTON_VD:
			if (showVD) {
				final Commune commune = service.getCommuneByNumeroOfsEtendu(localisation.getNoOfs(), null);
				if (commune == null) {
					return "Commune vaudoise inconnue";
				}
				else {
					return commune.getNomMinuscule();
				}
			}
			else {
				return "";
			}
		case HORS_CANTON: {
			final Commune commune = service.getCommuneByNumeroOfsEtendu(localisation.getNoOfs(), null);
			if (commune == null) {
				return "Commune hors-canton inconnue";
			}
			else {
				return commune.getNomMinuscule() + " (" + commune.getSigleCanton() + ")";
			}
		}
		case HORS_SUISSE: {
			return getPays(localisation.getNoOfs());
		}
		default:
			throw new IllegalArgumentException("Type de localisation inconnue = [" + localisation + "]");
		}
	}

	private String getPays(int noOfs) {
		final Pays pays = service.getPays(noOfs);
		if (pays == null) {
			return "Pays inconnu";
		}
		else {
			return pays.getNomMinuscule();
		}
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

	public void setShowVD(boolean showVD) {
		this.showVD = showVD;
	}

	public void setLocalisation(LocalisationView localisation) {
		this.localisation = localisation;
	}

	public void setService(ServiceInfrastructureService service) {
		JspTagLocalisation.service = service;
	}
}
