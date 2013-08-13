package ch.vd.uniregctb.common;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public class MultipleSwitchTest extends WithoutSpringTest {

	private static class SimpleSwitchable implements Switchable {
		private boolean enabled;

		private SimpleSwitchable(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}
	}

	private Switchable[] buildSwitchables(boolean... state) {
		final Switchable[] array = new Switchable[state.length];
		for (int i = 0 ; i < array.length ; ++ i) {
			array[i] = new SimpleSwitchable(state[i]);
		}
		return array;
	}

	@Test
	public void testSetEnabled() throws Exception {
		final boolean[] states = {true, false, true, false, false};
		final Switchable[] array = buildSwitchables(states);
		final MultipleSwitch multiple = new MultipleSwitch(array);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		multiple.setEnabled(true);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.setEnabled(false);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertFalse("position " + i, array[i].isEnabled());
		}
	}

	@Test
	public void testPushPop() throws Exception {
		final boolean[] states = {true, false, true, false, false};
		final Switchable[] array = buildSwitchables(states);
		final MultipleSwitch multiple = new MultipleSwitch(array);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		multiple.pushState();
		multiple.setEnabled(true);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.pushState();
		multiple.setEnabled(false);
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertFalse("position " + i, array[i].isEnabled());
		}
		multiple.popState();
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertTrue("position " + i, array[i].isEnabled());
		}
		multiple.popState();
		for (int i = 0 ; i < array.length ; ++ i) {
			Assert.assertEquals("position " + i, states[i], array[i].isEnabled());
		}
		try {
			multiple.popState();
			Assert.fail("Il y a encore des trucs dedans ?");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}
	}
}
