package ch.vd.uniregctb.web.xt.component;

import org.apache.commons.lang.StringEscapeUtils;
import org.springmodules.xt.ajax.component.Component;

public class SimpleText implements Component {

    private static final long serialVersionUID = 26L;

    private final String text;

    /**
     * Construct the component.
     * @param text The string to wrap.
     */
    public SimpleText(String text) {
    	this.text = (text == null ? "" :StringEscapeUtils.unescapeHtml(text));
    }

    public String render() {
    	if ( text == null)
    		return "";
        return  StringEscapeUtils.escapeXml(this.text);
    }
}