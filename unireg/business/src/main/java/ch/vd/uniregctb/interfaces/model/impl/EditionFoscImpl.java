package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EditionFoscImpl implements EditionFosc {

	private final int annee;
	private final int numero;

	public static EditionFoscImpl get(ch.vd.registre.pm.model.EditionFosc target) {
		if (target == null) {
			return null;
		}
		return new EditionFoscImpl(target);
	}

	public EditionFoscImpl(ch.vd.registre.pm.model.EditionFosc target) {
		this.annee = target.getAnnee();
		this.numero = target.getNumero();
	}

	@Override
	public int getAnnee() {
		return annee;
	}

	@Override
	public int getNumero() {
		return numero;
	}
}
