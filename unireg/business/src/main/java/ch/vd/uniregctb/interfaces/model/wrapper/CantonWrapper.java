package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Canton;

public class CantonWrapper extends EntiteOFSWrapper implements Canton, Serializable {

	private static final long serialVersionUID = -6354759399676227008L;
	
	public static CantonWrapper get(ch.vd.infrastructure.model.Canton target) {
		if (target == null) {
			return null;
		}
		return new CantonWrapper(target);
	}

	private CantonWrapper(ch.vd.infrastructure.model.Canton target) {
		super(target);
	}
}
