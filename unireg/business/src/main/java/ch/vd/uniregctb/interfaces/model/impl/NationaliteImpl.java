package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Pays;

public class NationaliteImpl implements Nationalite, Serializable {

	private static final long serialVersionUID = -78753304926530147L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;
	private final int noSequence;

	public static NationaliteImpl get(ch.vd.registre.civil.model.Nationalite target) {
		if (target == null) {
			return null;
		}
		return new NationaliteImpl(target);
	}

	private NationaliteImpl(ch.vd.registre.civil.model.Nationalite target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noSequence = target.getNoSequence();
		this.pays = PaysImpl.get(target.getPays());
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public Pays getPays() {
		return pays;
	}

}
