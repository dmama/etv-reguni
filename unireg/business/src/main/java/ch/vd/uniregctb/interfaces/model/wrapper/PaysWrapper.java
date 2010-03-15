package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class PaysWrapper extends EntiteOFSWrapper implements Pays {

	public static PaysWrapper get(ch.vd.infrastructure.model.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysWrapper(target);
	}

	private PaysWrapper(ch.vd.infrastructure.model.Pays target) {
		super(target);
	}

	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}
}
