package ch.vd.uniregctb.interfaces.model;

import org.jetbrains.annotations.Nullable;

/**
 * Interface d'adresse qui peut être liée à une commune directement
 */
public interface AdresseAvecCommune {

	/**
	 * Retourne le numéro Ofs de la commune de l'adresse
	 * @return le numéro Ofs de la commune attachée à cette adresse
	 */
	@Nullable
	Integer getNoOfsCommuneAdresse();

	/**
	 * @return le numéro Ofs de bâtiment (Gebäude) ou <b>null</b> s'il est inconnu.
	 */
	Integer getEgid();

	/**
	 * @return le numéro Ofs de logement (Wohnung) ou <b>null</b> s'il est inconnu.
	 */
	Integer getEwid();
}
