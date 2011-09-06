package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Pays;

public class NationaliteImpl implements Nationalite, Serializable {

	private static final long serialVersionUID = 134684618996801933L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;

	public static NationaliteImpl get(ch.vd.registre.civil.model.Nationalite target) {
		if (target == null) {
			return null;
		}
		return new NationaliteImpl(target);
	}

	private NationaliteImpl(ch.vd.registre.civil.model.Nationalite target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.pays = PaysImpl.get(target.getPays());
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

}
