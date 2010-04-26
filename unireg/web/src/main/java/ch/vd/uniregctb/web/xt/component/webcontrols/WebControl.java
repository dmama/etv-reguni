package ch.vd.uniregctb.web.xt.component.webcontrols;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.HtmlTextWriterAttribute;
import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.xt.component.Attribute;
import ch.vd.uniregctb.web.xt.component.Control;

public class WebControl extends Control {

	private static final long serialVersionUID = 4103127758878265888L;

	private HtmlTextWriterTag tag;

	protected final Map<String, Attribute> attributes = new HashMap<String, Attribute>();
	protected final Map<String, String> styleAttributes = new HashMap<String, String>();

	public WebControl() {
	}

	public WebControl(HtmlTextWriterTag tag) {
		super();
		this.tag = tag;
	}

	@Override
	public String getId() {
		return getAttributeValue(HtmlTextWriterAttribute.Id);
	}

	public void setId(String id) {
		addAttribute(HtmlTextWriterAttribute.Id, id);
	}

	public String getCssClass() {
		return getAttributeValue(HtmlTextWriterAttribute.Class);
	}

	public void setCssClass( String value) {
		addAttribute(HtmlTextWriterAttribute.Class, value);
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return getAttributeValue(HtmlTextWriterAttribute.Title);
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String value) {
		addAttribute(HtmlTextWriterAttribute.Title, value);
	}

	/**
	 * Add a generic attribute.
	 *
	 * @param name
	 *            The attribute name.
	 * @param value
	 *            The attribute value.
	 */
	final public void addAttribute(String name, String value) {
		this.attributes.put(name, new Attribute(name, value));
	}

	final public void addAttribute(HtmlTextWriterAttribute name, String value) {
		addAttribute(name.name(), value);
	}

	final public void addStyleAttribute(String name, String value) {
		this.styleAttributes.put(name, value);
	}

	final public void addStyleAttribute(HtmlTextWriterAttribute name, String value) {
		this.styleAttributes.put(name.name(), value);
	}

	public String getTagName() {
		return tag.name();
	}

	protected void addAttributesToRender(HtmlTextWriter writer) {
		if (!this.attributes.isEmpty()) {
			for (Attribute attr : attributes.values()) {
				writer.addAttribute(attr.getName(), attr.getValue());
			}
		}
		if (!this.styleAttributes.isEmpty()) {
			for (Map.Entry<String, String> entry : styleAttributes.entrySet()) {
				writer.addStyleAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void renderBeginTag(HtmlTextWriter writer) {

		this.addAttributesToRender(writer);
		writer.renderBeginTag(tag);
	}

	@Override
	public void renderEndTag(HtmlTextWriter writer) {
		writer.renderEndTag();
	}

	protected String getAttributeValue(HtmlTextWriterAttribute attr) {
		return getAttributeValue(attr.name());
	}

	protected String getAttributeValue(String name) {
		Attribute attribute = this.attributes.get(name);
		if ( attribute != null) {
			return attribute.getValue();
		}
		return null;
	}



}
