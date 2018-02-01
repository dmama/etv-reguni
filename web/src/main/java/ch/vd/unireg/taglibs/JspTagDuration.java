package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.common.TimeHelper;

/**
 * Tag JSP qui permet d'afficher une durée, exprimée techniquement en millisecondes, d'une manière humainement plus lisible
 */
public class JspTagDuration extends BodyTagSupport {

	private Long msDuration;
	private boolean isNullDuration;
	private boolean rounded = false;
	private boolean shortVersion = false;
	private String zeroDisplay;
	private String nullDisplay;

	public void setMsDuration(Long msDuration) {
		this.msDuration = msDuration;
	}

	public void setIsNullDuration(boolean isNullDuration) {
		this.isNullDuration = isNullDuration;
	}

	public void setRounded(boolean rounded) {
		this.rounded = rounded;
	}

	public void setShortVersion(boolean shortVersion) {
		this.shortVersion = shortVersion;
	}

	public void setZeroDisplay(String zeroDisplay) {
		this.zeroDisplay = zeroDisplay;
	}

	public void setNullDisplay(String nullDisplay) {
		this.nullDisplay = nullDisplay;
	}

	@Override
	public int doStartTag() throws JspException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			return SKIP_BODY;
		}
		catch (IOException e) {
			throw new JspTagException(e);
		}
	}

	private String buildHtml() {
		return buildDisplayText(isNullDuration ? null : msDuration, nullDisplay, zeroDisplay, shortVersion, rounded);
	}

	/**
	 * Méthode publique pour la construction d'une expression HTML valide qui représente la durée donnée
	 * @param msDuration durée, en millisecondes
	 * @param nullDisplay chaîne utilisée si la durée donnée est <code>null</code>
	 * @param zeroDisplay chaîne utilisée si la durée donnée est 0 ou négative
	 * @param shortVersion <code>true</code> si la durée doit être affichée en version "courte", <code>false</code> si elle doit l'être en version "longue"
	 * @param rounded <code>true</code> si la durée doit être arrondie (sinon, elle est affichée à la seconde près)
	 * @return la chaîne HTML qui va bien
	 */
	public static String buildDisplayText(Long msDuration, String nullDisplay, String zeroDisplay, boolean shortVersion, boolean rounded) {
		final String text;
		if (msDuration == null) {
			text = StringUtils.trimToEmpty(nullDisplay);
		}
		else if (msDuration <= 0 && zeroDisplay != null) {
			text = StringUtils.trimToEmpty(zeroDisplay);
		}
		else if (shortVersion) {
			text = TimeHelper.formatDureeShort(msDuration);
		}
		else if (rounded) {
			text = TimeHelper.formatDureeArrondie(msDuration);
		}
		else {
			text = TimeHelper.formatDuree(msDuration);
		}
		return HtmlUtils.htmlEscape(text);
	}
}
