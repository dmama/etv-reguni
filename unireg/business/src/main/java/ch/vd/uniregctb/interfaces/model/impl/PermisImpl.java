package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.type.TypePermis;

public class PermisImpl implements Permis, Serializable {

	private static final long serialVersionUID = 2087648750531032890L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final int noSequence;
	private final TypePermis typePermis;

	public static PermisImpl get(ch.vd.registre.civil.model.Permis target) {
		if (target == null) {
			return null;
		}
		return new PermisImpl(target);
	}

	private PermisImpl(ch.vd.registre.civil.model.Permis target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.dateAnnulation = RegDate.get(target.getDateAnnulation());
		this.noSequence = target.getNoSequence();
		this.typePermis = TypePermis.get(target.getTypePermis());
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
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	@Override
	public int getNoSequence() {
		return noSequence;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}

}
