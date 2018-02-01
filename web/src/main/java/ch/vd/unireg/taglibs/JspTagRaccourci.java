package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.unireg.utils.WebContextUtils;

public class JspTagRaccourci extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = -8109093180921197899L;

	private static final String NBSP = "&nbsp;";

	private static final String HASH = "#";

	/**
	 * Ce membre est statique pour permettre l'injection par Spring du bean accessible par toutes
	 * les instances des tags de raccourcis
	 */
	private static MessageSource messageSource;

	private String onClick;

	private String display;

	private String tooltip;

	private String link;

	private String id;

	/**
	 * @see javax.servlet.jsp.tagext.BodyTagSupport#doStartTag()
	 */
	@Override
	public int doStartTag() throws JspException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public String getOnClick() {
		return onClick;
	}

	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String title) {
		this.tooltip = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String buildHtml() {
		final StringBuilder builder = new StringBuilder();
		builder.append("<a href=\"");
		builder.append(getHRef());
		builder.append("\" class=\"");
		builder.append(getCssClass());
		builder.append(" noprint");

		final String onClick = getOnClick();
		if (!estVide(onClick)) {
			builder.append("\" onClick=\"");
			builder.append(onClick);
		}

		final String tooltip = getTooltip();
		if (!estVide(tooltip)) {
			builder.append("\" title=\"");
			builder.append(getMessage(tooltip));
		}

		final String id = getId();
		if (!estVide(id)) {
			builder.append("\" id=\"");
			builder.append(id);
		}

		builder.append("\">");
		builder.append(getBody());
		builder.append("</a>");
		return builder.toString();
	}

	/**
	 * Le body du tag html "a" : ne jamais laisser vide (cela pose un problème à IE!)
	 */
	protected String getBody() {
		final String display = getDisplay();
		return estVide(display) ? NBSP : getMessage(display);
	}

	protected String getHRef() {
		final String link = getLink();
		return estVide(link) ? HASH : link;
	}

	private static boolean estVide(String str) {
		return str == null || str.length() == 0;
	}

	private static String getMessage(String key) {
		return messageSource.getMessage(key, null, WebContextUtils.getDefaultLocale());
	}

	/**
	 * Le nom de la classe à utiliser pour l'affichage de l'icône
	 */
	protected String getCssClass() {
		throw new RuntimeException("getCssClass() doit-être redéfini sur la classe " + getClass().getName());
	}

	/**
	 * @see org.springframework.context.MessageSourceAware#setMessageSource(org.springframework.context.MessageSource)
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		JspTagRaccourci.messageSource = messageSource;
	}

	/**
	 * Raccourci d'impression
	 */
	public static class Imprimer extends JspTagRaccourci {

		private static final long serialVersionUID = -6003446344537681460L;

		@Override
		protected String getCssClass() {
			return "printer";
		}
	}

	/**
	 * Raccourci d'annulation
	 */
	public static class Annuler extends JspTagRaccourci {

		private static final long serialVersionUID = -6280082382584390105L;

		@Override
		protected String getCssClass() {
			return "delete";
		}
	}

	/**
	 * Raccourci de modification
	 */
	public static class Modifier extends JspTagRaccourci {

		private static final long serialVersionUID = 8019720221487056881L;

		@Override
		protected String getCssClass() {
			return "edit";
		}
	}

	/**
	 * Raccourci d'enregistrement de données
	 */
	public static class Enregistrer extends JspTagRaccourci {

		private static final long serialVersionUID = 4514972534732668432L;

		@Override
		protected String getCssClass() {
			return "save";
		}
	}

	/**
	 * Raccourci d'ajout
	 */
	public static class Ajouter extends JspTagRaccourci {

		private static final long serialVersionUID = -6164066764766163614L;

		@Override
		protected String getCssClass() {
			return "add";
		}
	}

	/**
	 * Raccourci d'abandon de saisie
	 */
	public static class Abandonner extends JspTagRaccourci {

		private static final long serialVersionUID = 2574271931490030554L;

		@Override
		protected String getCssClass() {
			return "cancel iepngfix";
		}
	}

	/**
	 * Raccourci d'alerte
	 */
	public static class Alerter extends JspTagRaccourci {

		private static final long serialVersionUID = 4834725890031639757L;

		@Override
		protected String getCssClass() {
			return "alert iepngfix";
		}
	}

	/**
	 * Raccourci de démarrage
	 */
	public static class Demarrer extends JspTagRaccourci {

		private static final long serialVersionUID = -7874210263889713387L;

		@Override
		protected String getCssClass() {
			return "start";
		}
	}

	/**
	 * Raccourci d' arrêt
	 */
	public static class Arreter extends JspTagRaccourci {

		private static final long serialVersionUID = -7874210263009713387L;

		@Override
		protected String getCssClass() {
			return "stop iepngfix";
		}
	}

	/**
	 * Raccourci d'identification
	 */
	public static class Identifier extends JspTagRaccourci {

		private static final long serialVersionUID = 7812560634286434840L;

		@Override
		protected String getCssClass() {
			return "key iepngfix";
		}
	}

	/**
	 * Ré-initialisation
	 */
	public static class Reinit extends JspTagRaccourci {

		@Override
		protected String getCssClass() {
			return "replay";
		}
	}

	/**
	 * Monte d'un cran
	 */
	public static class MoveUp extends JspTagRaccourci {

		@Override
		protected String getCssClass() {
			return "moveUp";
		}
	}

	/**
	 * Descend d'un cran
	 */
	public static class MoveDown extends JspTagRaccourci {

		@Override
		protected String getCssClass() {
			return "moveDown";
		}
	}

	/**
	 * Ré-ouverture
	 */
	public static class ReOpen extends JspTagRaccourci {

		@Override
		protected String getCssClass() {
			return "reOpen";
		}
	}

	/**
	 * Détails (= loupe)
	 */
	public static class Detail extends JspTagRaccourci {
		@Override
		protected String getCssClass() {
			return "detail";
		}
	}

	/**
	 * Déplier (= plus)
	 */
	public static class Deplier extends JspTagRaccourci {
		@Override
		protected String getCssClass() {
			return "deplier";
		}
	}

	/**
	 * Plier (= moins)
	 */
	public static class Plier extends JspTagRaccourci {
		@Override
		protected String getCssClass() {
			return "plier";
		}
	}

	/**
	 * Historique (= page de calendrier)
	 */
	public static class Historique extends JspTagRaccourci {
		@Override
		protected String getCssClass() {
			return "historique";
		}
	}
}
