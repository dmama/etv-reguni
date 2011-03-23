package ch.vd.uniregctb.admin;

import org.springmodules.xt.ajax.component.Component;

/**
 * Table spécialisée pour l'affichage de la progression d'un job en pourcent
 */
public class JobPercentIndicator implements Component {

	private static final long serialVersionUID = -8857731698878141582L;

	private final int percent;

	public JobPercentIndicator(int percent) {
		this.percent = percent;
	}

	public String render() {
		int width = 100;
		final int pixels = (width * percent) / 100;
		StringBuilder s = new StringBuilder();
		s.append("<div class=\"progress-bar\" style=\"width: ").append(width).append("px\">");
		s.append("<div class=\"progress-bar-fill\" style=\"width: ").append(pixels).append("px\"></div>");
		s.append("<div class=\"progress-bar-text\" style=\"width: ").append(width).append("px\">").append(percent).append("%</div></div>");
		return s.toString();
	}
}
