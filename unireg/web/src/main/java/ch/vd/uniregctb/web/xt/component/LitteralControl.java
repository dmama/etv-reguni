package ch.vd.uniregctb.web.xt.component;

import org.apache.commons.lang.StringEscapeUtils;

import ch.vd.uniregctb.web.HtmlTextWriter;

public class LitteralControl extends Control {

	private static final long serialVersionUID = -6543383608837085448L;

	private String text;

	public LitteralControl(String text) {
		this.text = text;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void renderContent(HtmlTextWriter writer) {
		if ( text != null)
			writer.write(StringEscapeUtils.escapeXml(text));
	}

}
