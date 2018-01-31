package ch.vd.uniregctb.common;

import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * Un élément qui s'active et se désactive dans le thread courant
 */
public class ThreadSwitch implements Switchable {

	private final boolean initialValue;

	private final ThreadLocal<MutableBoolean> byThreadValue = new ThreadLocal<MutableBoolean>() {
		@Override
		protected MutableBoolean initialValue() {
			return new MutableBoolean(initialValue);
		}
	};

	public ThreadSwitch(boolean initialValue) {
		this.initialValue = initialValue;
	}

	private MutableBoolean getByThreadValue() {
		return byThreadValue.get();
	}

	@Override
	public void setEnabled(boolean value) {
		getByThreadValue().setValue(value);
	}

	@Override
	public boolean isEnabled() {
		return getByThreadValue().booleanValue();
	}
}
