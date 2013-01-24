package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.uniregctb.type.TypeAdressePM;

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
	TypeAdressePM getType();

	/**
	 * Retourne le complément d'adresse.
	 * @return le complément d'adresse
	 */
	String getComplement();

	/**
	 * Retourne le numero technique de la rue.
	 *
	 * @return la rue de l'adresse.
	 */
	Integer getNumeroTechniqueRue();

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
	 * Retourne le numéro d'ordre postal de la localité.
	 *
	 * @return le numéro d'ordre postal de la localité.
	 */
	int getNumeroOrdrePostal();

	/**
	 * @return le numéro postal d'acheminement de la localité. Par exemple, "1000" pour Lausanne.
	 */
	String getNumeroPostal();

	/**
     * @return le numéro postal complémentaire de l'adresse (principalement pour les localités étrangères).
     */
	String getNumeroPostalComplementaire();

	/**
	 * @return Le nom de la localite abrégé en minuscule
	 */
	String getLocaliteAbregeMinuscule();

	/**
	 * @return Le nom de la localite complet en minuscule.
	 */
	String getLocaliteCompletMinuscule();
	
	/**
	 * @return le pays de l'adresse; ou <code>null</code> si l'adresse est située en Suisse.
	 */
	Pays getPays();
}
