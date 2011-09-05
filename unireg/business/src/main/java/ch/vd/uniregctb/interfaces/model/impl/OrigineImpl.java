package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class OrigineImpl implements Origine, Serializable {

	private static final long serialVersionUID = -3210113924960027602L;
	
	private final Commune commune;
	private final Pays pays;

	public static OrigineImpl get(ch.vd.registre.civil.model.Origine target) {
		if (target == null) {
			return null;
		}
		return new OrigineImpl(target);
	}

	private OrigineImpl(ch.vd.registre.civil.model.Origine target) {
		this.commune = CommuneImpl.get(target.getCommune());
		this.pays = PaysImpl.get(target.getPays());
	}

	@Override
	public String getNomLieu() {
		return commune == null ? null : commune.getNomMinuscule();
	}

	@Override
	public String getSigleCanton() {
		return commune == null ? null : commune.getSigleCanton();
	}

	@Override
	public Pays getPays() {
		return pays;
	}

}
