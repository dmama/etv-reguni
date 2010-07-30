package ch.vd.uniregctb.web;

import java.util.Hashtable;
import java.util.Map;

import ch.vd.uniregctb.web.io.TextWriter;

public class XhtmlTextWriter extends HtmlTextWriter {
	Map<String, Boolean> common_attrs = new Hashtable<String, Boolean>(DefaultCommonAttributes.length);
	Map<String, Boolean> suppress_common_attrs = new Hashtable<String, Boolean>(DefaultSuppressCommonAttributes.length);
	Map<String, Map<String, Boolean>> element_specific_attrs = new Hashtable<String, Map<String, Boolean>>();

	Map<String, Boolean> attr_render = new Hashtable<String, Boolean>();

	// XhtmlMobileDocType doc_type;

	final static String[] DefaultCommonAttributes = {
			"class", "id", "title", "xml:lang"
	};

	//
	// XHTML elements whose CommonAttributes are supressed
	//
	final static String[] DefaultSuppressCommonAttributes = {
			"base", "meta", "br", "head", "title", "html", "style"
	};

	public XhtmlTextWriter(TextWriter writer) {
		this(writer, DefaultTabString);

	}

	public XhtmlTextWriter(TextWriter writer, String tabString) {
		super(writer, tabString);
		setupCommonAttributes();
		setupSuppressCommonAttributes();
		setupElementsSpecificAttributes();
	}

	void setupHash(Map<String, Boolean> hash, String[] values) {
		for (String str : values) {
			hash.put(str, true);
		}
	}

	//
	// if you need to add a new default common attribute,
	// add the literal as a member of the DefaultCommonAttributes array
	//
	void setupCommonAttributes() {
		setupHash(common_attrs, DefaultCommonAttributes);
	}

	//
	// if you need to add a new suppressed common attribute,
	// add the literal as a member of the SuppressCommonAttrs array
	//
	void setupSuppressCommonAttributes() {
		setupHash(suppress_common_attrs, DefaultSuppressCommonAttributes);
	}

	//
	// I did not make them static because different instances of XhtmlTextWriter's
	// do not share the changes made to the element's attributes tables,
	// they are not read-only.
	//
	Map<String, Boolean> a_attrs, base_attrs, blockquote_attrs, br_attrs, form_attrs, head_attrs;
	Map<String, Boolean> html_attrs, img_attrs, input_attrs, label_attrs, li_attrs, link_attrs;
	Map<String, Boolean> meta_attrs, object_attrs, ol_attrs, optgroup_attrs, option_attrs, param_attrs;
	Map<String, Boolean> pre_attrs, q_attrs, select_attrs, style_attrs, table_attrs, textarea_attrs;
	Map<String, Boolean> td_attrs, th_attrs, title_attrs, tr_attrs;

	void setupElementsSpecificAttributes() {
		String[] a_attrs_names = {
				"accesskey", "href", "charset", "hreflang", "rel", "type", "rev", "title", "tabindex"
		};
		setupElementSpecificAttributes("a", a_attrs, a_attrs_names);

		String[] base_attrs_names = {
			"href"
		};
		setupElementSpecificAttributes("base", base_attrs, base_attrs_names);

		String[] blockquote_attrs_names = {
			"cite"
		};
		setupElementSpecificAttributes("blockquote", blockquote_attrs, blockquote_attrs_names);

		String[] br_attrs_names = {
				"id", "class", "title"
		};
		setupElementSpecificAttributes("br", br_attrs, br_attrs_names);

		String[] form_attrs_names = {
				"action", "method", "enctype"
		};
		setupElementSpecificAttributes("form", form_attrs, form_attrs_names);

		String[] head_attrs_names = {
			"xml:lang"
		};
		setupElementSpecificAttributes("head", head_attrs, head_attrs_names);

		String[] html_attrs_names = {
				"version", "xml:lang", "xmlns"
		};
		setupElementSpecificAttributes("html", html_attrs, html_attrs_names);

		String[] img_attrs_names = {
				"src", "alt", "width", "longdesc", "height"
		};
		setupElementSpecificAttributes("img", img_attrs, img_attrs_names);

		String[] input_attrs_names = {
				"size", "accesskey", "title", "name", "type", "disabled", "value", "src", "checked", "maxlength", "tabindex"
		};
		setupElementSpecificAttributes("input", input_attrs, input_attrs_names);

		String[] label_attrs_names = {
				"accesskey", "for"
		};
		setupElementSpecificAttributes("label", label_attrs, label_attrs_names);

		String[] li_attrs_names = {
			"value"
		};
		setupElementSpecificAttributes("li", li_attrs, li_attrs_names);

		String[] link_attrs_names = {
				"hreflang", "rev", "type", "charset", "rel", "href", "media"
		};
		setupElementSpecificAttributes("link", link_attrs, link_attrs_names);

		String[] meta_attrs_names = {
				"content", "name", "xml:lang", "http-equiv", "scheme"
		};
		setupElementSpecificAttributes("meta", meta_attrs, meta_attrs_names);

		String[] object_attrs_names = {
				"codebase", "classid", "data", "standby", "name", "type", "height", "archive", "declare", "width", "tabindex", "codetype"
		};
		setupElementSpecificAttributes("object", object_attrs, object_attrs_names);

		String[] ol_attrs_names = {
			"start"
		};
		setupElementSpecificAttributes("ol", ol_attrs, ol_attrs_names);

		String[] optgroup_attrs_names = {
				"label", "disabled"
		};
		setupElementSpecificAttributes("optgroup", optgroup_attrs, optgroup_attrs_names);

		String[] option_attrs_names = {
				"selected", "value"
		};
		setupElementSpecificAttributes("option", option_attrs, option_attrs_names);

		String[] param_attrs_names = {
				"id", "name", "valuetype", "value", "type"
		};
		setupElementSpecificAttributes("param", param_attrs, param_attrs_names);

		String[] pre_attrs_names = {
			"xml:space"
		};
		setupElementSpecificAttributes("pre", pre_attrs, pre_attrs_names);

		String[] q_attrs_names = {
			"cite"
		};
		setupElementSpecificAttributes("q", q_attrs, q_attrs_names);

		String[] select_attrs_names = {
				"name", "tabindex", "disabled", "multiple", "size"
		};
		setupElementSpecificAttributes("select", select_attrs, select_attrs_names);

		String[] style_attrs_names = {
				"xml:lang", "xml:space", "type", "title", "media"
		};
		setupElementSpecificAttributes("style", style_attrs, style_attrs_names);

		String[] table_attrs_names = {
				"width", "summary"
		};
		setupElementSpecificAttributes("table", table_attrs, table_attrs_names);

		String[] textarea_attrs_names = {
				"name", "cols", "accesskey", "tabindex", "rows"
		};
		setupElementSpecificAttributes("textarea", textarea_attrs, textarea_attrs_names);

		String[] td_and_th_attrs_names = {
				"headers", "align", "rowspan", "colspan", "axis", "scope", "abbr", "valign"
		};
		setupElementSpecificAttributes("td", td_attrs, td_and_th_attrs_names);
		setupElementSpecificAttributes("th", th_attrs, td_and_th_attrs_names);

		String[] title_attrs_names = {
			"xml:lang"
		};
		setupElementSpecificAttributes("title", title_attrs, title_attrs_names);

		String[] tr_attrs_names = {
				"align", "valign"
		};
		setupElementSpecificAttributes("tr", tr_attrs, tr_attrs_names);
	}

	void setupElementSpecificAttributes(String elementName, Map<String, Boolean> attrs, String[] attributesNames) {
		attrs = new Hashtable<String, Boolean>(attributesNames.length);
		initElementAttributes(attrs, attributesNames);
		element_specific_attrs.put(elementName, attrs);
	}

	void initElementAttributes(Map<String, Boolean> attrs, String[] attributesNames) {
		setupHash(attrs, attributesNames);
	}

	protected Map<String, Boolean> getCommonAttributes() {
		return common_attrs;
	}

	protected Map<String, Map<String, Boolean>> getElementSpecificAttributes() {
		return element_specific_attrs;
	}

	protected Map<String, Boolean> getSuppressCommonAttributes() {
		return suppress_common_attrs;
	}

	public void AddRecognizedAttribute(String elementName, String attributeName) {
		Map<String, Boolean> elem_attrs = element_specific_attrs.get(elementName);

		if (elem_attrs == null) {
			Map<String, Boolean> attrs = new Hashtable<String, Boolean>();
			attrs.put(attributeName, true);
			element_specific_attrs.put(elementName, attrs);
		}
		else
			elem_attrs.put(attributeName, true);
	}

	@Override
	public boolean isValidFormAttribute(String attributeName) {
		return attributeName.equals("action") || attributeName.equals("method") || attributeName.equals("enctype");
	}

	public void removeRecognizedAttribute(String elementName, String attributeName) {
		Map<String, Boolean> elem_attrs = element_specific_attrs.get(elementName);

		if (elem_attrs != null)
			elem_attrs.remove(attributeName);
	}

	public void setDocType(Object docType) {
		// doc_type = docType;
	}

	// writes <br/>
	@Override
	public void writeBreak() {
		String tag = getTagName(HtmlTextWriterTag.Br);
		writeBeginTag(tag);
		write(SlashChar);
		write(TagRightChar);
	}

	protected boolean OnAttributeRender(String name, String value, HtmlTextWriterAttribute key) {
		return attr_render.get(null);
	}

	@Override
	protected boolean onStyleAttributeRender(String name, String value, HtmlTextWriterStyle key) {
		return false;
	}

}
