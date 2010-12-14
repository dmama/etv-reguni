package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.uniregctb.utils.WebContextUtils;

public class JspTagRaccourci extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = -8430389192865302785L;

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

	private boolean thickbox;

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

	public boolean isThickbox() {
		return thickbox;
	}

	public void setThickbox(boolean thickbox) {
		this.thickbox = thickbox;
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
		if (isThickbox()) {
			builder.append(" thickbox");
		}

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
		return estVide(display) ? NBSP : NBSP + getMessage(display);
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
	public void setMessageSource(MessageSource messageSource) {
		JspTagRaccourci.messageSource = messageSource;
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

}
