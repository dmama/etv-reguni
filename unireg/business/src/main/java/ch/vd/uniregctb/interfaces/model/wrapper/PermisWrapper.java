package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Permis;

public class PermisWrapper implements Permis {

	private final ch.vd.registre.civil.model.Permis target;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateAnnulation;

	public static PermisWrapper get(ch.vd.registre.civil.model.Permis target) {
		if (target == null) {
			return null;
		}
		return new PermisWrapper(target);
	}

	private PermisWrapper(ch.vd.registre.civil.model.Permis target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.dateAnnulation = RegDate.get(target.getDateAnnulation());
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
		return target.getNoSequence();
	}

	public EnumTypePermis getTypePermis() {
		return target.getTypePermis();
	}

}
