package ch.vd.uniregctb.common;

/**
 * Un élément qui s'active et se désactive dans le thread courant
 */
public class ThreadSwitch implements Switchable {

	private static class Behavior {
		public boolean enabled;
		public Behavior(boolean enabled) {
			this.enabled = enabled;
		}
	}

	private final boolean initialValue;

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>() {
		@Override
		protected Behavior initialValue() {
			return new Behavior(initialValue);
		}
	};

	public ThreadSwitch(boolean initialValue) {
		this.initialValue = initialValue;
	}

	private Behavior getByThreadBehavior() {
		return byThreadBehavior.get();
	}

	@Override
	public void setEnabled(boolean value) {
		getByThreadBehavior().enabled = value;
	}

	@Override
	public boolean isEnabled() {
		return getByThreadBehavior().enabled;
	}
}
