package ch.vd.uniregctb.norentes.webcontrols;

import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.HtmlTextWriterAttribute;
import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.xt.component.webcontrols.WebControl;

public class ToolbarButton extends WebControl {

	private static final long serialVersionUID = 3184929193013924068L;
	private boolean enabled = true;
	private String imageUrl;
	private String imageDisabledUrl;
	private String linkUrl;
	private String text;

	public ToolbarButton() {
		super(HtmlTextWriterTag.Span);
	}

	@Override
	protected void onInit() {
		super.onInit();
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @param imageUrl
	 *            the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the imageDisabledUrl
	 */
	public String getImageDisabledUrl() {
		return imageDisabledUrl;
	}

	/**
	 * @param imageDisabledUrl
	 *            the imageDisabledUrl to set
	 */
	public void setImageDisabledUrl(String imageDisabledUrl) {
		this.imageDisabledUrl = imageDisabledUrl;
	}

	/**
	 * @return the linkUrl
	 */
	public String getLinkUrl() {
		return linkUrl;
	}

	/**
	 * @param linkUrl
	 *            the linkUrl to set
	 */
	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	@Override
	protected void onPreRender() {
		super.onPreRender();
		if (!enabled) {
			setCssClass("ToolbarButton disabled");
		}
		else {
			setCssClass("ToolbarButton");
		}
	}

	@Override
	public void render(HtmlTextWriter writer) {
		if (getText() == null && getImageUrl() == null) {
			return;
		}
		super.render(writer);
	}

	@Override
	protected void addAttributesToRender(HtmlTextWriter writer) {
		if (getCssClass() != null) {
			writer.addAttribute(HtmlTextWriterAttribute.Class, getCssClass());
		}
	}

	@Override
	public void renderContent(HtmlTextWriter writer) {
		if (enabled) {
			writer.addAttribute(HtmlTextWriterAttribute.Title, getTitle());
			writer.addAttribute(HtmlTextWriterAttribute.Href, getLinkUrl());
			writer.addAttribute(HtmlTextWriterAttribute.Id, getId());
			writer.renderBeginTag(HtmlTextWriterTag.A);
		}
		renderImage(writer);
		if (getText() != null) {
			writer.renderBeginTag(HtmlTextWriterTag.Span);
			writer.write(getText());
			writer.renderEndTag(); // tag SPAN
		}
		if (enabled) {
			writer.renderEndTag(); // tag A
		}
	}

	private void renderImage(HtmlTextWriter writer) {
		if (getImageUrl() == null) {
			return;
		}
		String url = getImageUrl();
		if (!enabled) {
			if (getImageDisabledUrl() != null)
				url = getImageDisabledUrl();
		}
		writer.addAttribute(HtmlTextWriterAttribute.Src, url);
		writer.addAttribute(HtmlTextWriterAttribute.Align, "top");
		writer.renderBeginTag(HtmlTextWriterTag.Img);
		writer.renderEndTag(); // tag IMG
	}

}
