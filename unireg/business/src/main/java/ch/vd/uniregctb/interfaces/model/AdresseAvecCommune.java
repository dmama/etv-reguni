package ch.vd.uniregctb.interfaces.model;

/**
 * Interface d'adresse qui peut être liée à une commune directement
 */
public interface AdresseAvecCommune {

	/**
	 * Retourne la commune de l'adresse
	 * @return la commune attachée à cette adresse
	 */
	CommuneSimple getCommuneAdresse();
}
