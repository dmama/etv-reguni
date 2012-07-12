package ch.vd.uniregctb.identification.individus;

import java.util.List;

import ch.vd.unireg.interfaces.civil.data.Individu;

public interface StrategieIdentification {

	String getNom();

	/**
	 * Recherche le ou les individus RcPers qui correspondent à l'individu RegPP spécifié en paramètre.
	 *
	 * @param individu un individu RegPP
	 * @return les numéros des individus RcPers identifiés.
	 */
	List<Long> identifieIndividuRcPers(Individu individu);
}
