package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

public class JspTagInteroperabilite extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = 5038749015565554902L;

	private static final String CAT = "CAT";

	private long noTiers;
	private NatureTiers natureTiers;
	private boolean debiteurInactif;

	private static MessageSource messageSource;

	/**
	 * Mapping entre les noms des modalités de {@link NatureTiers} (tous en majuscules) et la modalité elle-même
	 * pour pouvoir récupérer l'information de manière insensible à la casse (ce qui est mis dans l'indexeur est systématiquement mis en minuscules, par exemple)
	 */
	private static final Map<String, NatureTiers> naturesTiers = buildNaturesTiersMap();

	private static final Map<String, UrlBuilder> urlBuilders = buildUrlBuildersMap();

	private static Map<String, UrlBuilder> buildUrlBuildersMap() {
		final Map<String, UrlBuilder> map = new HashMap<>();
		for (ApplicationFiscale app : ApplicationFiscale.values()) {
			map.put(app.name(), new AppFiscaleUrlBuilder(app));
		}
		map.put(CAT, new CatUrlBuilder());
		return Collections.unmodifiableMap(map);
	}

	private static Map<String, NatureTiers> buildNaturesTiersMap() {
		final Map<String, NatureTiers> map = new HashMap<>();
		for (NatureTiers modalite : NatureTiers.values()) {
			map.put(StringUtils.upperCase(modalite.name()), modalite);
		}
		return Collections.unmodifiableMap(map);
	}

	@Override
	public int doStartTag() throws JspException {
		try {
			final JspWriter out = pageContext.getOut();
			final String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
			out.print(buildHtml(contextPath, natureTiers, noTiers, debiteurInactif));
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	private static interface UrlBuilder {
		String url(String contextPath, long noTiers);
	}

	private static class AppFiscaleUrlBuilder implements UrlBuilder {
		private final ApplicationFiscale app;

		private AppFiscaleUrlBuilder(ApplicationFiscale app) {
			this.app = app;
		}

		@Override
		public String url(String contextPath, long noTiers) {
			return String.format("%s/redirect/%s.do?id=%d", contextPath, app.name(), noTiers);
		}
	}

	private static class CatUrlBuilder implements UrlBuilder {
		@Override
		public String url(String contextPath, long noTiers) {
			return String.format("%s/tiers/launchcat.do?numero=%d", contextPath, noTiers);
		}
	}

	private static String addOption(String contextPath, long noTiers, ApplicationFiscale app) {
		final String url = urlBuilders.get(app.name()).url(contextPath, noTiers);
		return addOption(url, app.getMessageKey());
	}

	private static String addOptionCat(String contextPath, long noTiers) {
		final String url = urlBuilders.get(CAT).url(contextPath, noTiers);
		return addOption(url, "label.CAT");
	}

	private static String addOption(String url, String messageKey) {
		return String.format("\t<option value=\"%s\">%s</option>\n", url, message(messageKey));
	}

	public static String buildHtml(String contextPath, NatureTiers natureTiers, long noTiers, boolean debiteurInactif) {
		final StringBuilder b = new StringBuilder();

		b.append("<select name=\"AppSelect\" onchange=\"App.gotoExternalApp(this);\">\n");
		b.append("\t<option value=\"\">---</option>\n");

		final Set<ApplicationFiscale> apps = getApplicationsFiscalesAutorisees(natureTiers, debiteurInactif);
		for (ApplicationFiscale app : apps) {
			b.append(addOption(contextPath, noTiers, app));
		}
		if (natureTiers != NatureTiers.Entreprise) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
			b.append(addOptionCat(contextPath, noTiers));
		}
		b.append("</select>\n");
		return b.toString();
	}

	public static Set<ApplicationFiscale> getApplicationsFiscalesAutorisees(NatureTiers natureTiers, boolean debiteurInactif) {
		final Set<NatureTiers> naturesTiersPP = EnumSet.of(NatureTiers.Habitant, NatureTiers.NonHabitant, NatureTiers.MenageCommun);
		final boolean isEntreprise = natureTiers == NatureTiers.Entreprise;
		final boolean isPP = naturesTiersPP.contains(natureTiers);
		final boolean showTAO = !isEntreprise && !debiteurInactif;
		final boolean showSIPF = true;
		final boolean showREPELEC = !debiteurInactif && isPP;

		final Set<ApplicationFiscale> apps = EnumSet.noneOf(ApplicationFiscale.class);
		if (showTAO) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
			apps.add(ApplicationFiscale.TAO_PP);
			apps.add(ApplicationFiscale.TAO_BA);
			apps.add(ApplicationFiscale.TAO_IS);
		}
		if (showSIPF) {
			apps.add(ApplicationFiscale.SIPF);
		}
		if (showREPELEC) {
			apps.add(ApplicationFiscale.REPELEC);
		}
		return apps;
	}

	private static String message(String key) {
		return JspTagInteroperabilite.messageSource.getMessage(key, null, WebContextUtils.getDefaultLocale());
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		JspTagInteroperabilite.messageSource = messageSource;
	}

	public void setNoTiers(long noTiers) {
		this.noTiers = noTiers;
	}

	public void setNatureTiers(String natureTiers) {
		final NatureTiers modalite = naturesTiers.get(StringUtils.upperCase(natureTiers));
		if (modalite == null) {
			throw new IllegalArgumentException("Illegal value : " + natureTiers);
		}
		this.natureTiers = modalite;
	}

	public void setDebiteurInactif(boolean debiteurInactif) {
		this.debiteurInactif = debiteurInactif;
	}
}
