package ch.vd.uniregctb.norentes.webcontrols;

import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.xt.component.webcontrols.WebControl;

public class Toolbar extends WebControl {

	private static final long serialVersionUID = -4841319476695625216L;

	public Toolbar() {
		super(HtmlTextWriterTag.Div);
	}

	@Override
	protected void onPreRender() {
		super.onPreRender();
		setCssClass("toolbar-control");
	}

}
