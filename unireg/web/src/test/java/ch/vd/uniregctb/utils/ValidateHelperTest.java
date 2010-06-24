package ch.vd.uniregctb.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit4ClassRunner.class)
public class ValidateHelperTest {

	@Test
	public void testIsPositiveInteger() {
		assertFalse(ValidateHelper.isPositiveInteger(null));
		assertFalse(ValidateHelper.isPositiveInteger(""));
		assertFalse(ValidateHelper.isPositiveInteger("QWE"));
		assertFalse(ValidateHelper.isPositiveInteger("-QWE"));
		assertFalse(ValidateHelper.isPositiveInteger("-2"));
		assertFalse(ValidateHelper.isPositiveInteger("-0"));
		assertFalse(ValidateHelper.isPositiveInteger("0.12"));
		assertFalse(ValidateHelper.isPositiveInteger("1.0.12"));

		assertTrue(ValidateHelper.isPositiveInteger("000"));
		assertTrue(ValidateHelper.isPositiveInteger("1"));
		assertTrue(ValidateHelper.isPositiveInteger("1234567890"));
	}
}
