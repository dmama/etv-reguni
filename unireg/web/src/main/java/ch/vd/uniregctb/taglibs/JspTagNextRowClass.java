package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.mutable.MutableInt;

/**
 * Jsp tag qui retourne alternativement les classes 'odd' et 'even' à chaque appel.
 */
public class JspTagNextRowClass extends BodyTagSupport {

	private static final long serialVersionUID = 1L;

	private static final ThreadLocal<MutableInt> count = new ThreadLocal<MutableInt>();
	private static final ThreadLocal<Boolean> justReset = new ThreadLocal<Boolean>();

	public JspTagNextRowClass() {
		if (count.get() == null) {
			count.set(new MutableInt(0));
		}
		if (justReset.get() == null) {
			justReset.set(false);
		}
	}

	@Override
	public int doStartTag() throws JspTagException {

		if (justReset.get()) {
			justReset.set(false);
			return SKIP_BODY;
		}

		try {
			final JspWriter out = pageContext.getOut();
			if (count.get().intValue() % 2 == 0) {
				out.print("even");
			}
			else {
				out.print("odd");
			}
			count.get().increment();
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setReset(int value) {
		count.get().setValue(value);
		justReset.set(true);
	}
}