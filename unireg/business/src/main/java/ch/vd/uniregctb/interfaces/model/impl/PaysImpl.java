package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class PaysImpl extends EntiteOFSImpl implements Pays, Serializable {

	private static final long serialVersionUID = -6750309642346732586L;

	public static PaysImpl get(ch.vd.infrastructure.model.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target);
	}

	private PaysImpl(ch.vd.infrastructure.model.Pays target) {
		super(target);
	}

	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}
}
