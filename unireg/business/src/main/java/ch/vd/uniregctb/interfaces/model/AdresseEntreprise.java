package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;

public interface AdresseEntreprise {

	/**
	 * Retourne la date de début de validité de l'adresse.
	 * @return la date de début de validité de l'adresse
	 */
	RegDate getDateDebutValidite();

	/**
	 * Retourne la date de fin de validité de l'adresse.
	 * @return la date de fin de validité de l'adresse
	 */
	RegDate getDateFinValidite();

	/**
	 * Retourne le type de l'adresse (Courrier, Siège ou facturation).
	 * @return le type de l'adresse (Courrier, Siège ou facturation)
	 */
	EnumTypeAdresseEntreprise getType();

	/**
	 * Retourne le complément d'adresse.
	 * @return le complément d'adresse
	 */
	String getComplement();

	/**
	 * Retourne le nom de la rue.
	 * @return le nom de la rue
	 */
	String getRue();

	/**
	 * Retourne le numéro de maison.
	 * @return le numéro de maison.
	 */
	String getNumeroMaison();

	/**
	 * Retourne le numéro postal d'acheminement et le nom de la localité postale (ou le contenu du champ lieu pour les adresses à l'étranger).
	 * @return le numéro postal d'acheminement et le nom de la localité postale (ou le contenu du champ lieu pour les adresses à l'étranger)
	 */
	String getLocalite();
}
