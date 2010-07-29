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

	@Override
	public int doStartTag() throws JspTagException {

		if (justReset()) {
			setJustReset(false);
			return SKIP_BODY;
		}

		try {
			final JspWriter out = pageContext.getOut();
			if (getCount() % 2 == 0) {
				out.print("even");
			}
			else {
				out.print("odd");
			}
			incCount();
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	private void setJustReset(boolean value) {
		justReset.set(value);
	}

	public void setReset(int value) {
		setCount(value);
		setJustReset(true);
	}

	private void setCount(int value) {
		if (count.get() == null) {
			count.set(new MutableInt(value));
		}
		else {
			count.get().setValue(value);
		}
	}

	private int getCount() {
		if (count.get() == null) {
			count.set(new MutableInt(0));
		}
		return count.get().intValue();
	}

	private void incCount() {
		count.get().increment();
	}

	private boolean justReset() {
		return justReset.get() != null && justReset.get();
	}
}