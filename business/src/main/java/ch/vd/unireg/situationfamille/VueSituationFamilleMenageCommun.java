package ch.vd.uniregctb.situationfamille;

import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * Vue de la situation de famille spécifique aux ménages communs.
 */
public interface VueSituationFamilleMenageCommun extends VueSituationFamille {

	/**
	 * @return le tarif impôt-source applicable, ou <b>null</b> si ce tarif n'est pas applicable.
	 */
	TarifImpotSource getTarifApplicable();

	/**
	 * @return le numéro du contribuable (personne physique) principal dans le ménage commun, ou <b>null</b> si le tarif impôt-source n'est
	 *         pas applicable.
	 */
	Long getNumeroContribuablePrincipal();
}
