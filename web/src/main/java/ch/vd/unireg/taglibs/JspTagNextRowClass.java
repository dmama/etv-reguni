package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Jsp tag qui retourne alternativement les classes 'odd' et 'even' à chaque appel.
 */
public class JspTagNextRowClass extends BodyTagSupport {

	private static final long serialVersionUID = -6368239797943548141L;

	private static final ThreadLocal<MutableInt> count = ThreadLocal.withInitial(() -> new MutableInt(0));
	private static final ThreadLocal<Boolean> justReset = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private boolean frozen = false;

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
			if (!frozen) {
				incCount();
			}
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

	@SuppressWarnings("UnusedDeclaration")
	public void setReset(int value) {
		setCount(value);
		setJustReset(true);
	}

	private void setCount(int value) {
		count.get().setValue(value);
	}

	private int getCount() {
		return count.get().intValue();
	}

	private void incCount() {
		count.get().increment();
	}

	private boolean justReset() {
		return justReset.get();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}
}