package ch.vd.uniregctb.web;

import java.util.Hashtable;

import org.apache.commons.lang.NotImplementedException;

import ch.vd.uniregctb.web.controls.IStyle;
import ch.vd.uniregctb.web.io.TextWriter;

public class HtmlTextWriter extends TextWriter {

    static HtmlTag[] tags = { new HtmlTag(HtmlTextWriterTag.Unknown, "", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.A, "a", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Acronym, "acronym", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Address, "address", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Area, "area", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.B, "b", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Base, "base", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Basefont, "basefont", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Bdo, "bdo", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Bgsound, "bgsound", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Big, "big", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Blockquote, "blockquote", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Body, "body", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Br, "br", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Button, "button", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Caption, "caption", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Center, "center", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Cite, "cite", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Code, "code", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Col, "col", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Colgroup, "colgroup", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Dd, "dd", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Del, "del", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Dfn, "dfn", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Dir, "dir", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Div, "div", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Dl, "dl", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Dt, "dt", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Em, "em", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Embed, "embed", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Fieldset, "fieldset", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Font, "font", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Form, "form", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Frame, "frame", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Frameset, "frameset", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H1, "h1", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H2, "h2", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H3, "h3", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H4, "h4", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H5, "h5", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.H6, "h6", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Head, "head", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Hr, "hr", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Html, "html", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.I, "i", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Iframe, "iframe", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Img, "img", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Input, "input", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Ins, "ins", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Isindex, "isindex", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Kbd, "kbd", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Label, "label", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Legend, "legend", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Li, "li", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Link, "link", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Map, "map", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Marquee, "marquee", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Menu, "menu", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Meta, "meta", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Nobr, "nobr", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Noframes, "noframes", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Noscript, "noscript", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Object, "object", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Ol, "ol", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Option, "option", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.P, "p", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Param, "param", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Pre, "pre", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Q, "q", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Rt, "rt", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Ruby, "ruby", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.S, "s", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Samp, "samp", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Script, "script", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Select, "select", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Small, "small", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Span, "span", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Strike, "strike", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Strong, "strong", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Style, "style", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Sub, "sub", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Sup, "sup", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Table, "table", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Tbody, "tbody", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Td, "td", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Textarea, "textarea", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Tfoot, "tfoot", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Th, "th", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Thead, "thead", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Title, "title", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Tr, "tr", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Tt, "tt", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.U, "u", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Ul, "ul", TagType.Block),
            new HtmlTag(HtmlTextWriterTag.Var, "var", TagType.Inline),
            new HtmlTag(HtmlTextWriterTag.Wbr, "wbr", TagType.SelfClosing),
            new HtmlTag(HtmlTextWriterTag.Xml, "xml", TagType.Block), };

    static HtmlAttribute[] htmlattrs = { new HtmlAttribute(HtmlTextWriterAttribute.Accesskey, "accesskey"),
            new HtmlAttribute(HtmlTextWriterAttribute.Align, "align"),
            new HtmlAttribute(HtmlTextWriterAttribute.Alt, "alt"),
            new HtmlAttribute(HtmlTextWriterAttribute.Background, "background"),
            new HtmlAttribute(HtmlTextWriterAttribute.Bgcolor, "bgcolor"),
            new HtmlAttribute(HtmlTextWriterAttribute.Border, "border"),
            new HtmlAttribute(HtmlTextWriterAttribute.Bordercolor, "bordercolor"),
            new HtmlAttribute(HtmlTextWriterAttribute.Cellpadding, "cellpadding"),
            new HtmlAttribute(HtmlTextWriterAttribute.Cellspacing, "cellspacing"),
            new HtmlAttribute(HtmlTextWriterAttribute.Checked, "checked"),
            new HtmlAttribute(HtmlTextWriterAttribute.Class, "class"),
            new HtmlAttribute(HtmlTextWriterAttribute.Cols, "cols"),
            new HtmlAttribute(HtmlTextWriterAttribute.Colspan, "colspan"),
            new HtmlAttribute(HtmlTextWriterAttribute.Disabled, "disabled"),
            new HtmlAttribute(HtmlTextWriterAttribute.For, "for"),
            new HtmlAttribute(HtmlTextWriterAttribute.Height, "height"),
            new HtmlAttribute(HtmlTextWriterAttribute.Href, "href"),
            new HtmlAttribute(HtmlTextWriterAttribute.Id, "id"),
            new HtmlAttribute(HtmlTextWriterAttribute.Maxlength, "maxlength"),
            new HtmlAttribute(HtmlTextWriterAttribute.Multiple, "multiple"),
            new HtmlAttribute(HtmlTextWriterAttribute.Name, "name"),
            new HtmlAttribute(HtmlTextWriterAttribute.Nowrap, "nowrap"),
            new HtmlAttribute(HtmlTextWriterAttribute.Onchange, "onchange"),
            new HtmlAttribute(HtmlTextWriterAttribute.Onclick, "onclick"),
            new HtmlAttribute(HtmlTextWriterAttribute.ReadOnly, "readonly"),
            new HtmlAttribute(HtmlTextWriterAttribute.Rows, "rows"),
            new HtmlAttribute(HtmlTextWriterAttribute.Rowspan, "rowspan"),
            new HtmlAttribute(HtmlTextWriterAttribute.Rules, "rules"),
            new HtmlAttribute(HtmlTextWriterAttribute.Selected, "selected"),
            new HtmlAttribute(HtmlTextWriterAttribute.Size, "size"),
            new HtmlAttribute(HtmlTextWriterAttribute.Src, "src"),
            new HtmlAttribute(HtmlTextWriterAttribute.Style, "style"),
            new HtmlAttribute(HtmlTextWriterAttribute.Tabindex, "tabindex"),
            new HtmlAttribute(HtmlTextWriterAttribute.Target, "target"),
            new HtmlAttribute(HtmlTextWriterAttribute.Title, "title"),
            new HtmlAttribute(HtmlTextWriterAttribute.Type, "type"),
            new HtmlAttribute(HtmlTextWriterAttribute.Valign, "valign"),
            new HtmlAttribute(HtmlTextWriterAttribute.Value, "value"),
            new HtmlAttribute(HtmlTextWriterAttribute.Width, "width"),
            new HtmlAttribute(HtmlTextWriterAttribute.Wrap, "wrap"),
            new HtmlAttribute(HtmlTextWriterAttribute.Abbr, "abbr"),
            new HtmlAttribute(HtmlTextWriterAttribute.AutoComplete, "autocomplete"),
            new HtmlAttribute(HtmlTextWriterAttribute.Axis, "axis"),
            new HtmlAttribute(HtmlTextWriterAttribute.Content, "content"),
            new HtmlAttribute(HtmlTextWriterAttribute.Coords, "coords"),
            new HtmlAttribute(HtmlTextWriterAttribute.DesignerRegion, "_designerregion"),
            new HtmlAttribute(HtmlTextWriterAttribute.Dir, "dir"),
            new HtmlAttribute(HtmlTextWriterAttribute.Headers, "headers"),
            new HtmlAttribute(HtmlTextWriterAttribute.Longdesc, "longdesc"),
            new HtmlAttribute(HtmlTextWriterAttribute.Rel, "rel"),
            new HtmlAttribute(HtmlTextWriterAttribute.Scope, "scope"),
            new HtmlAttribute(HtmlTextWriterAttribute.Shape, "shape"),
            new HtmlAttribute(HtmlTextWriterAttribute.Usemap, "usemap"),
            new HtmlAttribute(HtmlTextWriterAttribute.VCardName, "vcard_name"),

    };

    static HtmlStyle[] htmlstyles = { new HtmlStyle(HtmlTextWriterStyle.BackgroundColor, "background-color"),
            new HtmlStyle(HtmlTextWriterStyle.BackgroundImage, "background-image"),
            new HtmlStyle(HtmlTextWriterStyle.BorderCollapse, "border-collapse"),
            new HtmlStyle(HtmlTextWriterStyle.BorderColor, "border-color"),
            new HtmlStyle(HtmlTextWriterStyle.BorderStyle, "border-style"),
            new HtmlStyle(HtmlTextWriterStyle.BorderWidth, "border-width"),
            new HtmlStyle(HtmlTextWriterStyle.Color, "color"),
            new HtmlStyle(HtmlTextWriterStyle.FontFamily, "font-family"),
            new HtmlStyle(HtmlTextWriterStyle.FontSize, "font-size"),
            new HtmlStyle(HtmlTextWriterStyle.FontStyle, "font-style"),
            new HtmlStyle(HtmlTextWriterStyle.FontWeight, "font-weight"),
            new HtmlStyle(HtmlTextWriterStyle.Height, "height"),
            new HtmlStyle(HtmlTextWriterStyle.TextDecoration, "text-decoration"),
            new HtmlStyle(HtmlTextWriterStyle.Width, "width"),
            new HtmlStyle(HtmlTextWriterStyle.ListStyleImage, "list-style-image"),
            new HtmlStyle(HtmlTextWriterStyle.ListStyleType, "list-style-type"),
            new HtmlStyle(HtmlTextWriterStyle.Cursor, "cursor"),
            new HtmlStyle(HtmlTextWriterStyle.Direction, "direction"),
            new HtmlStyle(HtmlTextWriterStyle.Display, "display"), new HtmlStyle(HtmlTextWriterStyle.Filter, "filter"),
            new HtmlStyle(HtmlTextWriterStyle.FontVariant, "font-variant"),
            new HtmlStyle(HtmlTextWriterStyle.Left, "left"), new HtmlStyle(HtmlTextWriterStyle.Margin, "margin"),
            new HtmlStyle(HtmlTextWriterStyle.MarginBottom, "margin-bottom"),
            new HtmlStyle(HtmlTextWriterStyle.MarginLeft, "margin-left"),
            new HtmlStyle(HtmlTextWriterStyle.MarginRight, "margin-right"),
            new HtmlStyle(HtmlTextWriterStyle.MarginTop, "margin-top"),
            new HtmlStyle(HtmlTextWriterStyle.Overflow, "overflow"),
            new HtmlStyle(HtmlTextWriterStyle.OverflowX, "overflow-x"),
            new HtmlStyle(HtmlTextWriterStyle.OverflowY, "overflow-y"),
            new HtmlStyle(HtmlTextWriterStyle.Padding, "padding"),
            new HtmlStyle(HtmlTextWriterStyle.PaddingBottom, "padding-bottom"),
            new HtmlStyle(HtmlTextWriterStyle.PaddingLeft, "padding-left"),
            new HtmlStyle(HtmlTextWriterStyle.PaddingRight, "padding-right"),
            new HtmlStyle(HtmlTextWriterStyle.PaddingTop, "padding-top"),
            new HtmlStyle(HtmlTextWriterStyle.Position, "position"),
            new HtmlStyle(HtmlTextWriterStyle.TextAlign, "text-align"),
            new HtmlStyle(HtmlTextWriterStyle.VerticalAlign, "vertical-align"),
            new HtmlStyle(HtmlTextWriterStyle.TextOverflow, "text-overflow"),
            new HtmlStyle(HtmlTextWriterStyle.Top, "top"), new HtmlStyle(HtmlTextWriterStyle.Visibility, "visibility"),
            new HtmlStyle(HtmlTextWriterStyle.WhiteSpace, "white-space"),
            new HtmlStyle(HtmlTextWriterStyle.ZIndex, "z-index"), };

    public final static String DefaultTabString = "\t";

    public final static char DoubleQuoteChar = '"';

    public final static String EndTagLeftChars = "</";

    public final static char EqualsChar = '=';

    public final static String EqualsDoubleQuoteString = "=\"";

    public final static String SelfClosingChars = " /";

    public final static String SelfClosingTagEnd = " />";

    public final static char SemicolonChar = ';';

    public final static char SingleQuoteChar = '\'';

    public final static char SlashChar = '/';

    public final static char SpaceChar = ' ';

    public final static char StyleEqualsChar = ':';

    public final static char TagLeftChar = '<';

    public final static char TagRightChar = '>';

    final static InsensitiveCaseHashtable<HtmlTag> _tagTable;

    final static InsensitiveCaseHashtable<HtmlAttribute> _attributeTable;

    final static InsensitiveCaseHashtable<HtmlStyle> _styleTable;

    static {

        _tagTable = new InsensitiveCaseHashtable<HtmlTag>(tags.length); // , StringComparer.OrdinalIgnoreCase);
        _attributeTable = new InsensitiveCaseHashtable<HtmlAttribute>(htmlattrs.length); // ,
        // StringComparer.OrdinalIgnoreCase);
        _styleTable = new InsensitiveCaseHashtable<HtmlStyle>(htmlstyles.length); // ,
        // StringComparer.OrdinalIgnoreCase);

        for (HtmlTag tag : tags)
            _tagTable.put(tag.name, tag);

        for (HtmlAttribute attr : htmlattrs)
            _attributeTable.put(attr.name, attr);

        for (HtmlStyle style : htmlstyles)
            _styleTable.put(style.name, style);
    }

    public HtmlTextWriter(TextWriter writer) {
        b = writer;
        tab_string = DefaultTabString;
    }

    public HtmlTextWriter(TextWriter writer, String tabString) {
        b = writer;
        tab_string = tabString;
    }

    static String staticGetStyleName(HtmlTextWriterStyle styleKey) {
        if (styleKey.ordinal() < htmlstyles.length)
            return htmlstyles[styleKey.ordinal()].name;

        return null;
    }

    protected static void registerAttribute(String name, HtmlTextWriterAttribute key) {
    }

    protected static void registerStyle(String name, HtmlTextWriterStyle key) {
    }

    protected static void registerTag(String name, HtmlTextWriterTag key) {
    }

    public void addAttribute(HtmlTextWriterAttribute key, String value, boolean fEncode) {
        if (fEncode)
            value = HttpUtilities.HtmlAttributeEncode(value);

        addAttribute(getAttributeName(key), value, key);
    }

    public void addAttribute(HtmlTextWriterAttribute key, String value) {
        if ((key != HtmlTextWriterAttribute.Name) && (key != HtmlTextWriterAttribute.Id))
            value = HttpUtilities.HtmlAttributeEncode(value);

        addAttribute(getAttributeName(key), value, key);
    }

    public void addAttribute(String name, String value, boolean fEncode) {
        if (fEncode)
            value = HttpUtilities.HtmlAttributeEncode(value);

        addAttribute(name, value, getAttributeKey(name));
    }

    public void addAttribute(String name, String value) {
        HtmlTextWriterAttribute key = getAttributeKey(name);

        if ((key != HtmlTextWriterAttribute.Name) && (key != HtmlTextWriterAttribute.Id))
            value = HttpUtilities.HtmlAttributeEncode(value);

        addAttribute(name, value, key);
    }

    protected void addAttribute(String name, String value, HtmlTextWriterAttribute key) {
        nextAttrStack();
        if (attrs[attrs_pos] == null)
            attrs[attrs_pos] = new AddedAttr();
        attrs[attrs_pos].name = name;
        attrs[attrs_pos].value = value;
        attrs[attrs_pos].key = key;
    }

    protected void addStyleAttribute(String name, String value, HtmlTextWriterStyle key) {
        nextStyleStack();
        if (styles[styles_pos] == null)
            styles[styles_pos] = new AddedStyle();
        styles[styles_pos].name = name;
        value = HttpUtilities.HtmlAttributeEncode(value);
        styles[styles_pos].value = value;
        styles[styles_pos].key = key;
    }

    public void addStyleAttribute(String name, String value) {
        addStyleAttribute(name, value, getStyleKey(name));
    }

    public void addStyleAttribute(HtmlTextWriterStyle key, String value) {
        addStyleAttribute(getStyleName(key), value, key);
    }

    @Override
    public void close() {
        b.close();
    }

    protected String encodeAttributeValue(HtmlTextWriterAttribute attrKey, String value) {
        return HttpUtilities.HtmlAttributeEncode(value);
    }

    protected String EncodeAttributeValue(String value, boolean fEncode) {
        if (fEncode)
            return HttpUtilities.HtmlAttributeEncode(value);
        return value;
    }

    protected String EncodeUrl(String url) {
        return HttpUtilities.UrlPathEncode(url);
    }

    protected void filterAttributes() {
        AddedAttr style_attr = new AddedAttr();

        for (int i = 0; i <= attrs_pos; i++) {
            AddedAttr a = attrs[i];
            if (onAttributeRender(a.name, a.value, a.key)) {
                if (a.key == HtmlTextWriterAttribute.Style) {
                    style_attr = a;
                    continue;
                }

                writeAttribute(a.name, a.value, false);
            }
        }

        if (styles_pos != -1 || style_attr.value != null) {
            write(SpaceChar);
            write("style");
            write(EqualsDoubleQuoteString);

            for (int i = 0; i <= styles_pos; i++) {
                AddedStyle a = styles[i];
                if (onStyleAttributeRender(a.name, a.value, a.key)) {
                    if (a.key == HtmlTextWriterStyle.BackgroundImage) {
                        a.value = "url(" + HttpUtilities.UrlPathEncode(a.value) + ")";
                    }
                    writeStyleAttribute(a.name, a.value, false);
                }
            }
            if (style_attr.value != null) {
                write(style_attr.value);
            }
            write(DoubleQuoteChar);
        }

        styles_pos = attrs_pos = -1;
    }

    @Override
    public void flush() {
        b.flush();
    }

    protected HtmlTextWriterAttribute getAttributeKey(String attrName) {
        HtmlAttribute attribute = _attributeTable.get(attrName);
        if (attribute == null)
            return null;

        return attribute.key;
    }

    protected String getAttributeName(HtmlTextWriterAttribute attrKey) {
        if (attrKey.ordinal() < htmlattrs.length)
            return htmlattrs[attrKey.ordinal()].name;

        return null;
    }

    protected HtmlTextWriterStyle getStyleKey(String styleName) {
        HtmlStyle style = _styleTable.get(styleName);
        if (style == null)
            return null;

        return style.key;
    }

    protected String getStyleName(HtmlTextWriterStyle styleKey) {
        return staticGetStyleName(styleKey);
    }

    protected HtmlTextWriterTag getTagKey(String tagName) {
        HtmlTag tag = _tagTable.get(tagName);
        if (tag == null)
            return HtmlTextWriterTag.Unknown;

        return tag.key;
    }

    static String staticGetTagName(HtmlTextWriterTag tagKey) {
        if (tagKey.ordinal() < tags.length)
            return tags[tagKey.ordinal()].name;

        return null;
    }

    protected String getTagName(HtmlTextWriterTag tagKey) {
        if (tagKey.ordinal() < tags.length)
            return tags[tagKey.ordinal()].name;

        return null;
    }

    protected boolean isAttributeDefined(HtmlTextWriterAttribute key) {

        return getAttributeDefined(key) != null;
    }

    protected String getAttributeDefined(HtmlTextWriterAttribute key) {
        for (int i = 0; i <= attrs_pos; i++)
            if (attrs[i].key == key) {
                String value = attrs[i].value;
                return value;
            }

        return null;
    }

    protected boolean isStyleAttributeDefined(HtmlTextWriterStyle key) {
        return getStyleAttributeDefined(key) != null;
    }

    protected String getStyleAttributeDefined(HtmlTextWriterStyle key) {
        for (int i = 0; i <= styles_pos; i++)
            if (styles[i].key == key) {
                String value = styles[i].value;
                return value;
            }

        return null;
    }

    protected boolean onAttributeRender(String name, String value, HtmlTextWriterAttribute key) {
        return true;
    }

    protected boolean onStyleAttributeRender(String name, String value, HtmlTextWriterStyle key) {
        return true;
    }

    protected boolean onTagRender(String name, HtmlTextWriterTag key) {
        return true;
    }

    protected void outputTabs() {
        if (!newline)
            return;
        newline = false;

        for (int i = 0; i < getIndent(); i++)
            b.write(tab_string);
    }

    protected String popEndTag() {
        if (tagstack_pos == -1)
            throw new RuntimeException("InvalidOperationException ");

        String s = getTagName();
        tagstack_pos--;
        return s;
    }

    protected void pushEndTag(String endTag) {
        nextTagStack();
        setTagName(endTag);
    }

    void pushEndTag(HtmlTextWriterTag t) {
        nextTagStack();
        setTagKey(t);
    }

    protected String renderAfterContent() {
        return null;
    }

    protected String renderAfterTag() {
        return null;
    }

    protected String renderBeforeContent() {
        return null;
    }

    protected String renderBeforeTag() {
        return null;
    }

    public void renderBeginTag(String tagName) {
        if (!onTagRender(tagName, getTagKey(tagName)))
            return;

        pushEndTag(tagName);

        doBeginTag();
    }

    public void renderBeginTag(HtmlTextWriterTag tagKey) {
        if (!onTagRender(getTagName(tagKey), tagKey))
            return;

        pushEndTag(tagKey);

        doBeginTag();
    }

    void writeIfNotNull(String s) {
        if (s != null)
            write(s);
    }

    void doBeginTag() {
        writeIfNotNull(renderBeforeTag());
        writeBeginTag(getTagName());
        filterAttributes();

        HtmlTextWriterTag key = getTagKey().ordinal() < tags.length ? getTagKey() : HtmlTextWriterTag.Unknown;
        TagType type = tags[key.ordinal()].tag_type;
        if (type == TagType.Inline) {
            write(TagRightChar);
        } else if (type == TagType.Block) {
            write(TagRightChar);
            writeLine();
            setIndent(getIndent() + 1);
        } else if (type == TagType.SelfClosing) {
            write(SelfClosingTagEnd);
        }

        writeIfNotNull(renderBeforeContent());
    }

    public void renderEndTag() {
        writeIfNotNull(renderAfterContent());

        HtmlTextWriterTag key = getTagKey().ordinal() < tags.length ? getTagKey() : HtmlTextWriterTag.Unknown;
        TagType type = tags[key.ordinal()].tag_type;
        if (type == TagType.Inline) {
            writeEndTag(getTagName());
        } else if (type == TagType.Block) {
            setIndent(getIndent() - 1);
            writeLineNoTabs("");
            writeEndTag(getTagName());
        }
        writeIfNotNull(renderAfterTag());

        popEndTag();
    }

    public void writeAttribute(String name, String value, boolean fEncode) {
        write(SpaceChar);
        write(name);
        if (value != null) {
            write(EqualsDoubleQuoteString);
            value = EncodeAttributeValue(value, fEncode);
            write(value);
            write(DoubleQuoteChar);
        }
    }

    public void writeBeginTag(String tagName) {
        write(TagLeftChar);
        write(tagName);
    }

    public void writeEndTag(String tagName) {
        write(EndTagLeftChars);
        write(tagName);
        write(TagRightChar);
    }

    public void writeFullBeginTag(String tagName) {
        write(TagLeftChar);
        write(tagName);
        write(TagRightChar);
    }

    public void writeStyleAttribute(String name, String value) {
        writeStyleAttribute(name, value, false);
    }

    public void writeStyleAttribute(String name, String value, boolean fEncode) {
        write(name);
        write(StyleEqualsChar);
        write(EncodeAttributeValue(value, fEncode));
        write(SemicolonChar);
    }

    @Override
    public void write(char[] buffer, int index, int count) {
        outputTabs();
        b.write(buffer, index, count);
    }

    @Override
    public void write(double value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(char value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(char[] buffer) {
        outputTabs();
        b.write(buffer);
    }

    @Override
    public void write(int value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(String format, Object arg0) {
        outputTabs();
        b.write(format, arg0);
    }

    @Override
    public void write(String format, Object arg0, Object arg1) {
        outputTabs();
        b.write(format, arg0, arg1);
    }

    @Override
    public void write(String format, Object... args) {
        outputTabs();
        b.write(format, args);
    }

    @Override
    public void write(String s) {
        outputTabs();
        b.write(s);
    }

    @Override
    public void write(long value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(Object value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(float value) {
        outputTabs();
        b.write(value);
    }

    @Override
    public void write(boolean value) {
        outputTabs();
        b.write(value);
    }

    public void writeAttribute(String name, String value) {
        writeAttribute(name, value, false);
    }

    @Override
    public void writeLine(char value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine(long value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine(Object value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine(double value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine(char[] buffer, int index, int count) {
        outputTabs();
        b.writeLine(buffer, index, count);
        newline = true;
    }

    @Override
    public void writeLine(char[] buffer) {
        outputTabs();
        b.writeLine(buffer);
        newline = true;
    }

    @Override
    public void writeLine(boolean value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine() {
        outputTabs();
        b.writeLine();
        newline = true;
    }

    @Override
    public void writeLine(int value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    @Override
    public void writeLine(String format, Object arg0, Object arg1) {
        outputTabs();
        b.writeLine(format, arg0, arg1);
        newline = true;
    }

    @Override
    public void writeLine(String format, Object arg0) {
        outputTabs();
        b.writeLine(format, arg0);
        newline = true;
    }

    @Override
    public void writeLine(String format, Object... args) {
        outputTabs();
        b.writeLine(format, args);
        newline = true;
    }

    @Override
    public void writeLine(String s) {
        outputTabs();
        b.writeLine(s);
        newline = true;
    }

    @Override
    public void writeLine(float value) {
        outputTabs();
        b.writeLine(value);
        newline = true;
    }

    public void writeLineNoTabs(String s) {
        b.writeLine(s);
        newline = true;
    }

    int indent;

    public int getIndent() {
        return indent;
    }

    public void setIndent(int value) {
        indent = value;
    }

    public TextWriter getInnerWriter() {
        return b;
    }

    public void setInnerWriter(TextWriter value) {
        b = value;
    }

    @Override
    public String getNewLine() {
        return b.getNewLine();
    }

    @Override
    public void setNewLine(String value) {
        b.setNewLine(value);
    }

    protected HtmlTextWriterTag getTagKey() {
        if (tagstack_pos == -1)
            throw new RuntimeException("InvalidOperationException");

        return tagstack[tagstack_pos].key;
    }

    protected void setTagKey(HtmlTextWriterTag value) {
        if (tagstack[tagstack_pos] == null)
            tagstack[tagstack_pos] = new AddedTag();
        tagstack[tagstack_pos].key = value;
        tagstack[tagstack_pos].name = getTagName(value);
    }

    protected String getTagName() {
        if (tagstack_pos == -1)
            throw new RuntimeException("InvalidOperationException");

        return tagstack[tagstack_pos].name;
    }

    protected void setTagName(String value) {
        if (tagstack[tagstack_pos] == null)
            tagstack[tagstack_pos] = new AddedTag();
        tagstack[tagstack_pos].name = value;
        tagstack[tagstack_pos].key = getTagKey(value);
        if (tagstack[tagstack_pos].key != HtmlTextWriterTag.Unknown)
            tagstack[tagstack_pos].name = getTagName(tagstack[tagstack_pos].key);
    }

    TextWriter b;

    String tab_string;

    boolean newline;

    //
    // These emulate generic Stack <T>, since we can't use that ;-(. _pos is the current
    // element.IE, you edit blah [blah_pos]. I *really* want generics, sigh.
    //
    AddedStyle[] styles;

    AddedAttr[] attrs;

    AddedTag[] tagstack;

    int styles_pos = -1, attrs_pos = -1, tagstack_pos = -1;

    static class AddedTag {
        public String name;

        public HtmlTextWriterTag key;
    }

    static class AddedStyle {
        public String name;

        public HtmlTextWriterStyle key;

        public String value;
    }

    static class AddedAttr {
        public String name;

        public HtmlTextWriterAttribute key;

        public String value;
    }

    void nextStyleStack() {
        if (styles == null)
            styles = new AddedStyle[16];

        if (++styles_pos < styles.length)
            return;

        int nsize = styles.length * 2;
        AddedStyle[] ncontents = new AddedStyle[nsize];

        System.arraycopy(styles, 0, ncontents, 0, styles.length);
        styles = ncontents;
    }

    void nextAttrStack() {
        if (attrs == null)
            attrs = new AddedAttr[16];

        if (++attrs_pos < attrs.length)
            return;

        int nsize = attrs.length * 2;
        AddedAttr[] ncontents = new AddedAttr[nsize];

        System.arraycopy(attrs, 0, ncontents, 0, attrs.length);
        attrs = ncontents;
    }

    void nextTagStack() {
        if (tagstack == null)
            tagstack = new AddedTag[16];

        if (++tagstack_pos < tagstack.length)
            return;

        int nsize = tagstack.length * 2;
        AddedTag[] ncontents = new AddedTag[nsize];

        System.arraycopy(tagstack, 0, ncontents, 0, tagstack.length);
        tagstack = ncontents;
    }

    enum TagType {
        Block, Inline, SelfClosing,
    }

    static class HtmlTag {
        public HtmlTextWriterTag key;

        public String name;

        public TagType tag_type;

        public HtmlTag(HtmlTextWriterTag k, String n, TagType tt) {
            key = k;
            name = n;
            tag_type = tt;
        }
    }

    static class HtmlStyle {
        public HtmlTextWriterStyle key;

        public String name;

        public HtmlStyle(HtmlTextWriterStyle k, String n) {
            key = k;
            name = n;
        }
    }

    static class HtmlAttribute {
        public HtmlTextWriterAttribute key;

        public String name;

        public HtmlAttribute(HtmlTextWriterAttribute k, String n) {
            key = k;
            name = n;
        }
    }

    public boolean isValidFormAttribute(String attribute) {
        return true;
    }

    // writes <br />
    public void writeBreak() {
        String br = getTagName(HtmlTextWriterTag.Br);
        writeBeginTag(br);
        write(SelfClosingTagEnd);
    }

    public void writeEncodedText(String text) {
        write(HttpUtilities.htmlEncode(text));
    }

    public void writeEncodedUrl(String url) {
        // WriteUrlEncodedString (url, false);
        throw new NotImplementedException();
    }

    public void writeEncodedUrlParameter(String urlText) {
        // WriteUrlEncodedString (urlText, true);
        throw new NotImplementedException();
    }

    protected void writeUrlEncodedString(String text, boolean argument) {
        throw new NotImplementedException();
    }

    public void enterStyle(IStyle style) {
        throw new NotImplementedException();
    }

    public void enterStyle(IStyle style, HtmlTextWriterTag tag) {
        throw new NotImplementedException();
    }

    public void exitStyle(IStyle style) {
        throw new NotImplementedException();
    }

    public void exitStyle(IStyle style, HtmlTextWriterTag tag) {
        throw new NotImplementedException();
    }

    /**
     *
     * @author xcicfh
     *
     * @param <V>
     */
    final static class InsensitiveCaseHashtable<V> extends Hashtable<String, V> {

        /**
         *
         */
        private static final long serialVersionUID = -467030201794417675L;

        public InsensitiveCaseHashtable(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public InsensitiveCaseHashtable(int initialCapacity) {
            super(initialCapacity);
        }

        public InsensitiveCaseHashtable() {
            super();
        }

        @Override
        public synchronized V put(String key, V value) {
            return super.put(key.toLowerCase(), value);
        }

        @Override
        public synchronized V get(Object key) {
            return super.get(convertKey(key));
        }

        @Override
        public synchronized boolean containsKey(Object key) {
            return super.containsKey(convertKey(key));
        }

        private String convertKey(Object key) {
            if (key instanceof String) {
                return ((String) key).toLowerCase();
            }
            return null;
        }

    }
}
