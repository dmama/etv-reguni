package ch.vd.uniregctb.interfaces.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;

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
	 * @return la première ligne de la désignation complète de la PM.
	 */
	String getRaisonSociale1();

	/**
	 * @return la deuxième ligne de la désignation complète de la PM (peut-être nulle).
	 */
	String getRaisonSociale2();

	/**
	 * @return la troisième ligne de la désignation complète de la PM (peut-être nulle).
	 */
	String getRaisonSociale3();
	
	/**
	 * @return la date de constitution de l'entreprise
	 */
	RegDate getDateConstitution();

	/**
	 * @return la date de fin d'activité de l'entreprise
	 */
	RegDate getDateFinActivite();

	/**
	 * @return toutes les formes juridiques de la PM, triées par ordre chronologique croissant.
	 */
	List<FormeJuridique> getFormesJuridiques();
	
	/**
	 * @return le nom de la personne de contact
	 */
	String getNomContact();

	/**
	 * @return le numéro de téléphone de la personne de contact
	 */
	String getTelephoneContact();

	/**
	 * @return le numéro de télécopie de la personne de contact
	 */
	String getTelecopieContact();

	/**
	 * @return la désignation abrégée de la PM.
	 */
	String getDesignationAbregee();

	/**
	 * @return le titulaire du compte
	 */
	String getTitulaireCompte();

	/**
	 * @return la date de bouclement future.
	 */
	RegDate getDateBouclementFuture();

	/**
	 * @return le numéro IPMRO (une gommette pour celui qui sait résoudre l'acronyme)
	 */
	String getNumeroIPMRO();

	/**
	 * @return le ou les comptes bancaires connus de la PM.
	 */
	List<CompteBancaire> getComptesBancaires();

	/**
     * @return la liste des adresses de la personne morale
     */
    Collection<AdresseEntreprise> getAdresses();

	/**
	 * @return toutes les capitaux de la PM, triés par ordre chronologique croissant.
	 */
	List<Capital> getCapitaux();

	/**
	 * @return toutes les états de la PM, triés par ordre chronologique croissant.
	 */
	List<EtatPM> getEtats();

	/**
	 * @return toutes les régimes fiscaux vaudois de la PM, triés par ordre chronologique croissant.
	 */
	List<RegimeFiscal> getRegimesVD();

	/**
	 * @return toutes les régimes fiscaux suisse de la PM, triés par ordre chronologique croissant.
	 */
	List<RegimeFiscal> getRegimesCH();

	/**
	 * @return toutes les sièges de la PM, triés par ordre chronologique croissant.
	 */
	List<Siege> getSieges();

	/**
	 * @return toutes les assujettissement LIC de la PM, triés par ordre chronologique croissant.
	 */
	List<AssujettissementPM> getAssujettissementsLIC();

	/**
	 * @return toutes les assujettissement LIFD de la PM, triés par ordre chronologique croissant.
	 */
	List<AssujettissementPM> getAssujettissementsLIFD();

	/**
	 * @return toutes les fors fiscaux principaux de la PM, triés par ordre chronologique croissant.
	 */
	List<ForPM> getForsFiscauxPrincipaux();

	/**
	 * @return toutes les fors fiscaux secondaires de la PM, triés par ordre chronologique croissant.
	 */
	List<ForPM> getForsFiscauxSecondaires();

	/**
	 * @return tous les mandats de la PM, triés par ordre chronologique croissant.
	 */
	List<Mandat> getMandats();

	/**
	 * @return le numéro IDE (genre "CHE999999996"), si connu
	 */
	String getNumeroIDE();

	/**
	 * Copie les parties spécifiées à partir de la PM spécifiée.
	 *
	 * @param pm    la personne morale sur laquelle il faut copier les parties.
	 * @param parts les parties à copier.
	 */
	void copyPartsFrom(PersonneMorale pm, Set<PartPM> parts);

	/**
	 * Clone la PM en restreignant la liste des parties renseignées.
	 *
	 * @param parts les parties à renseigner.
	 * @return une nouvelles personne morale
	 */
	PersonneMorale clone(Set<PartPM> parts);
}
