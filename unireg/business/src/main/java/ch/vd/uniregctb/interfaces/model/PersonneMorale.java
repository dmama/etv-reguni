package ch.vd.uniregctb.interfaces.model;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumFormeJuridique;

public interface PersonneMorale {

	/**
	 * Retourne le numéro de l'entreprise.
	 * @return le numéro de l'entreprise
	 */
	long getNumeroEntreprise();

	/**
	 * Retourne la raison sociale.
	 * @return la raison sociale
	 */
	String getRaisonSociale();

	/**
	 * Retourne la date de constitution de l'entreprise.
	 * @return la date de constitution de l'entreprise
	 */
	RegDate getDateConstitution();

	/**
	 * Retourne la date de fin d'activité de l'entreprise.
	 * @return la date de fin d'activité de l'entreprise
	 */
	RegDate getDateFinActivite();

	/**
	 * Retourne la forme juridique courante.
	 * @return la forme juridique courante
	 */
	EnumFormeJuridique getFormeJuridique();

	/**
	 * Retourne le nom de la personne de contact.
	 * @return le nom de la personne de contact
	 */
	String getNomContact();

	/**
	 * Retourne le numéro de téléphone de la personne de contact.
	 * @return le numéro de téléphone de la personne de contact
	 */
	String getTelephoneContact();

	/**
	 * Retourne le numéro de télécopie de la personne de contact.
	 * @return le numéro de télécopie de la personne de contact
	 */
	String getTelecopieContact();

	/**
	 * Retourne le numero de compte bancaire ou de compte postal entreprise.
	 * @return le numero de compte bancaire ou de compte postal entreprise
	 */
	String getNumeroCompteBancaire();

	/**
	 * Retourne le titulaire du compte.
	 * @return le titulaire du compte
	 */
	String getTitulaireCompte();

    /**
     * Retourne la liste des adresses de la personne morale.
     *
     * @return la liste des adresses de la personne morale
     */
    Collection<AdresseEntreprise> getAdresses();
}
