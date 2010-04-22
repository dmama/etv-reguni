package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface Mandat {

	/**
	 * @return le code du mandat (généralement, soit C, G, S ou T)
	 */
	String getCode();

	/**
	 * @return la date de début de validité
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité; ou <i>null</i> s'il est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return le prénom de la personne de contact. Peut être nul.
	 */
	String getPrenomContact();

	/**
	 * @return le nom de la personne de contact. Peut être nul.
	 */
	String getNomContact();

	/**
	 * @return le numéro de téléphone de la personne de contact. Peut être nul.
	 */
	String getNoTelephoneContact();

	/**
	 * @return le numéro de fax de la personne de contact. Peut être nul.
	 */
	String getNoFaxContact();

	/**
	 * @return le numéro de CCP du mandataire. Peut être nul.
	 */
	String getCCP();

	/**
	 * @return le numéro de compte bancaire du mandataire. Peut être nul.
	 */
	String getCompteBancaire();

	/**
	 * @return le numéro d'IBAN du mandataire. Peut être nul.
	 */
	String getIBAN();

	/**
	 * @return le code Bic Swift du mandataire. Peut être nul.
	 */
	String getBicSwift();

	/**
	 * @return le numéro de l'instution financière associé aux coordonnées financières du mandataire. Peut être nul.
	 */
	Long getNumeroInstitutionFinanciere();

	/**
	 * @return le numéro d'identification du mandataire (à interpréter en fonction du type de mandataire).
	 */
	long getNumeroMandataire();

	/**
	 * @return le type de mandataire.
	 */
	TypeMandataire getTypeMandataire();
}