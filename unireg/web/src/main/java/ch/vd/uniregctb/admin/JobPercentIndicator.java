package ch.vd.uniregctb.admin;

import org.springmodules.xt.ajax.component.Component;

import ch.vd.uniregctb.taglibs.JspTagPercentIndicator;

/**
 * Table spécialisée pour l'affichage de la progression d'un job en pourcent
 */
public class JobPercentIndicator implements Component {

	private static final long serialVersionUID = -8857731698878141582L;

	private final int percent;

	public JobPercentIndicator(int percent) {
		this.percent = percent;
	}

	@Override
	public String render() {
		return JspTagPercentIndicator.render(100, percent);
	}
}
