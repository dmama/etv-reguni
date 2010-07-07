package ch.vd.uniregctb.norentes.webcontrols;

import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.HtmlTextWriterAttribute;
import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.xt.component.webcontrols.WebControl;

public class ToolbarSeparator extends WebControl {

	private static final long serialVersionUID = -4516038826695592118L;

	public ToolbarSeparator() {
		super(HtmlTextWriterTag.Span);
	}

	@Override
	protected void addAttributesToRender(HtmlTextWriter writer) {
		writer.addAttribute(HtmlTextWriterAttribute.Style, "border-left-style:solid;border-right-style:solid;border-left-width:1px;border-right-width:1px;border-color:#999999;border-right-color:#ffffff;width:2px;overflow:hidden;margin-left:2px;margin-right:2px;color:transparent;");
	}
}
