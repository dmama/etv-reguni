package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.evd0007.v1.ExtendedCanton;

public class CantonImpl extends EntiteOFSImpl implements Canton, Serializable {

	private static final long serialVersionUID = 5848752651673341481L;
	
	public static CantonImpl get(ExtendedCanton target) {
		if (target == null) {
			return null;
		}
		return new CantonImpl(target);
	}
	public static CantonImpl get(ch.vd.infrastructure.model.rest.Canton target) {
		if (target == null) {
			return null;
		}
		return new CantonImpl(target);
	}

	private CantonImpl(ch.vd.infrastructure.model.rest.Canton target) {
		super(target);
	}

	private CantonImpl(ExtendedCanton target) {
		super(target.getCanton().getCantonId(), target.getCanton().getCantonLongName(), target.getCanton().getCantonLongName(), target.getCanton().getCantonAbbreviation().value());
	}
}
