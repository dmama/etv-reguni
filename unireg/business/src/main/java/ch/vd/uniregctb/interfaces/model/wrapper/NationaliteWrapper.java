package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Pays;

public class NationaliteWrapper implements Nationalite {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private Pays pays = null;
	private ch.vd.infrastructure.model.Pays targetPays;
	private int noSequence;

	public static NationaliteWrapper get(ch.vd.registre.civil.model.Nationalite target) {
		if (target == null) {
			return null;
		}
		return new NationaliteWrapper(target);
	}

	private NationaliteWrapper(ch.vd.registre.civil.model.Nationalite target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noSequence = target.getNoSequence();
		this.targetPays = target.getPays();
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
		if (pays == null && targetPays != null) {
			pays = PaysWrapper.get(targetPays);
			targetPays = null;
		}
		return pays;
	}

}
