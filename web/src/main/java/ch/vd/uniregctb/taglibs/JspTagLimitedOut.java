package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.taglibs.standard.tag.common.core.OutSupport;

/**
 * Ce tag permet d'afficher une valeur et tronque sa représentation string si elle dépasse une certaine limite.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagLimitedOut extends OutSupport {

	private static final long serialVersionUID = -5337549995900594216L;

	private int limit;

	@Override
	public int doStartTag() throws JspTagException {
		try {

			final Object v;
			if (value == null) {
				v = def;
			}
			else {
				v = value;
			}
			// on converti la valeur en string
			final String s;
			if (v instanceof Reader) {
				Reader reader = (Reader) v;
				s = IOUtils.toString(reader);
			}
			else {
				s = (v == null ? "" : v.toString());
			}

			// on limite si nécessaire
			final String limited;
			if (s.length() > limit) {
				limited = s.substring(0, limit - 3) + "...";
			}
			else {
				limited = s;
			}

			out(pageContext, escapeXml, limited);

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	// for tag attribute
	public void setValue(Object value) {
		this.value = value;
	}

	// for tag attribute
	public void setDefault(String def) {
		this.def = def;
	}

	// for tag attribute
	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}
}
