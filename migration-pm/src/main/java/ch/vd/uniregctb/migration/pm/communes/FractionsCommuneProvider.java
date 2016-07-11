package ch.vd.uniregctb.migration.pm.communes;

import java.util.List;

import ch.vd.unireg.interfaces.infra.data.Commune;

/**
 * Quelques méthodes utiles pour passer d'une commune faîtière à ses fractions
 */
public interface FractionsCommuneProvider {

	/**
	 * @param faitiere une commune faitière
	 * @return la liste des fractions (triée par numéro OFS croissant)
	 * @throws IllegalArgumentException si la commune n'est pas faîtière
	 */
	List<Commune> getFractions(Commune faitiere);
}
