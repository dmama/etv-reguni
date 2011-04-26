package ch.vd.uniregctb.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit4ClassRunner.class)
public class ValidatorUtilsTest {

	@Test
	public void testIsPositiveInteger() {
		assertFalse(ValidatorUtils.isPositiveInteger(null));
		assertFalse(ValidatorUtils.isPositiveInteger(""));
		assertFalse(ValidatorUtils.isPositiveInteger("QWE"));
		assertFalse(ValidatorUtils.isPositiveInteger("-QWE"));
		assertFalse(ValidatorUtils.isPositiveInteger("-2"));
		assertFalse(ValidatorUtils.isPositiveInteger("-0"));
		assertFalse(ValidatorUtils.isPositiveInteger("0.12"));
		assertFalse(ValidatorUtils.isPositiveInteger("1.0.12"));

		assertTrue(ValidatorUtils.isPositiveInteger("000"));
		assertTrue(ValidatorUtils.isPositiveInteger("1"));
		assertTrue(ValidatorUtils.isPositiveInteger("1234567890"));
	}
}
