package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EditionFoscImpl implements EditionFosc {

	private final int annee;
	private final int numero;
	private final RegDate dateParution;

	public static EditionFoscImpl get(ch.vd.registre.pm.model.EditionFosc target) {
		if (target == null) {
			return null;
		}
		return new EditionFoscImpl(target);
	}

	public EditionFoscImpl(ch.vd.registre.pm.model.EditionFosc target) {
		this.annee = target.getAnnee();
		this.numero = target.getNumero();
		this.dateParution = RegDate.get(target.getDateParution());
	}

	@Override
	public int getAnnee() {
		return annee;
	}

	@Override
	public int getNumero() {
		return numero;
	}

	@Override
	public RegDate getDateParution() {
		return dateParution;
	}
}
