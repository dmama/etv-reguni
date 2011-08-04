package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class PaysImpl extends EntiteOFSImpl implements Pays, Serializable {

	private static final long serialVersionUID = -6750309642346732586L;

	private final boolean valide;
	private final boolean etatSouverain;
	private final String codeIso2;
	private final String codeIso3;

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
		this.codeIso2 = null; // cette information n'est pas disponible dans host-interface
		this.codeIso3 = null; // cette information n'est pas disponible dans host-interface
	}

	private PaysImpl(ch.vd.fidor.ws.v2.Pays target) {
		super(target.getOfsId(), target.getNomCourtFr().toUpperCase(), target.getNomCourtFr(), target.getIso2Id());
		this.valide = target.isValide();
		this.etatSouverain = target.isEtat() != null && target.isEtat();
		this.codeIso2 = target.getIso2Id();
		this.codeIso3 = target.getIso3Id();
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String getCodeIso2() {
		return codeIso2;
	}

	@Override
	public String getCodeIso3() {
		return codeIso3;
	}

	@Override
	public boolean isEtatSouverain() {
		return etatSouverain;
	}
}
