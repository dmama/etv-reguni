package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class CantonImpl extends EntiteOFSImpl implements Canton, Serializable {

	private static final long serialVersionUID = -6354759399676227008L;
	
	public static CantonImpl get(ch.vd.infrastructure.model.Canton target) {
		if (target == null) {
			return null;
		}
		return new CantonImpl(target);
	}

	private CantonImpl(ch.vd.infrastructure.model.Canton target) {
		super(target);
	}
}
