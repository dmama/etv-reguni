package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EditionFoscWrapper implements EditionFosc {

	private final int annee;
	private final int numero;

	public static EditionFoscWrapper get(ch.vd.registre.pm.model.EditionFosc target) {
		if (target == null) {
			return null;
		}
		return new EditionFoscWrapper(target);
	}

	public EditionFoscWrapper(ch.vd.registre.pm.model.EditionFosc target) {
		this.annee = target.getAnnee();
		this.numero = target.getNumero();
	}

	public int getAnnee() {
		return annee;
	}

	public int getNumero() {
		return numero;
	}
}
