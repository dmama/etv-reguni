package ch.vd.unireg.taglibs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JspTagFormatCurrencyTest {

	@Test
	public void testTag() {
		JspTagFormatCurrency tag = new JspTagFormatCurrency();
		tag.setValue(12.25);
		assertEquals("12.25", tag.buidHtlm());
	}
}