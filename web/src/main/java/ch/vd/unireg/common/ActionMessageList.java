package ch.vd.unireg.common;

import java.util.ArrayList;

public class ActionMessageList extends ArrayList<ActionMessage> {

	public boolean isActive() {
		return !isEmpty();
	}

	public void setActive(boolean active) {
		if (!active) {
			clear();
		}
	}
}
