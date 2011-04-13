package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class PaysImpl extends EntiteOFSImpl implements Pays, Serializable {

	private static final long serialVersionUID = -6750309642346732586L;

	private final boolean valide;
	private final boolean etatSouverain;

	public static PaysImpl get(ch.vd.infrastructure.model.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target);
	}

	public static PaysImpl get(ch.vd.fidor.ws.v2.Pays target) {
		if (target == null) {
			return null;
		}
		return new PaysImpl(target);
	}

	private PaysImpl(ch.vd.infrastructure.model.Pays target) {
		super(target);
		this.valide = true; // tous les pays retournés par host-interfaces sont valides
		this.etatSouverain = true; // cette information n'est pas disponible dans host-interface
	}

	private PaysImpl(ch.vd.fidor.ws.v2.Pays target) {
		super(target.getOfsId(), target.getNomCourtFr().toUpperCase(), target.getNomCourtFr(), target.getIso2Id());
		this.valide = target.isValide();
		this.etatSouverain = target.isEtat() != null && target.isEtat();
	}

	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}

	public boolean isValide() {
		return valide;
	}

	public boolean isEtatSouverain() {
		return etatSouverain;
	}
}
