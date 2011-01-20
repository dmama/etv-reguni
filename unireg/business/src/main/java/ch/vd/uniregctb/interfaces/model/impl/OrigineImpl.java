package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class OrigineImpl implements Origine, Serializable {

	private static final long serialVersionUID = -3210113924960027602L;
	
	private final RegDate dateDebut;
	private final Commune commune;
	private final Pays pays;

	public static OrigineImpl get(ch.vd.registre.civil.model.Origine target) {
		if (target == null) {
			return null;
		}
		return new OrigineImpl(target);
	}

	private OrigineImpl(ch.vd.registre.civil.model.Origine target) {
		this.dateDebut = RegDate.get(target.getDebutValidite());
		this.commune = CommuneImpl.get(target.getCommune());
		this.pays = PaysImpl.get(target.getPays());
	}

	public Commune getCommune() {
		return commune;
	}

	public RegDate getDebutValidite() {
		return dateDebut;
	}

	public Pays getPays() {
		return pays;
	}

}
