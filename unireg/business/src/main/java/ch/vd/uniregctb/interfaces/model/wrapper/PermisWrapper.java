package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Permis;

public class PermisWrapper implements Permis {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;
	private final int noSequence;
	private final EnumTypePermis typePermis;

	public static PermisWrapper get(ch.vd.registre.civil.model.Permis target) {
		if (target == null) {
			return null;
		}
		return new PermisWrapper(target);
	}

	private PermisWrapper(ch.vd.registre.civil.model.Permis target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.dateAnnulation = RegDate.get(target.getDateAnnulation());
		this.noSequence = target.getNoSequence();
		this.typePermis = target.getTypePermis();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public EnumTypePermis getTypePermis() {
		return typePermis;
	}

}
