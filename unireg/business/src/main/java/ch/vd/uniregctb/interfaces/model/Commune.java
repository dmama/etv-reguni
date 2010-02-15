package ch.vd.uniregctb.interfaces.model;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

public interface Commune extends EntiteOFS {

	/**
	 * Retourne la collectivité en charge de l'administration de la commune.
	 *
	 * @return la collectivité en charge de l'administration de la commune.
	 */
	CollectiviteAdministrative getAdminstreePar();

	/**
	 * Retourne la liste des collectivités administratives de la commune. Cette liste contient des objets de type
	 * {@link CollectiviteAdministrative}.
	 *
	 * @return la liste des collectivités administratives de la commune.
	 */
	List<CollectiviteAdministrative> getCollectivites();

	/**
	 * Retourne la date de début de validité de la commune.
	 *
	 * @return la date de début de validité de la commune.
	 */
	RegDate getDateDebutValidite();

	/**
	 * Retourne la date de fin de validité de la commune.
	 *
	 * @return la date de fin de validité de la commune.
	 */
	RegDate getDateFinValidite();

	/**
	 * Retourne le numéro ACI de la commune.
	 *
	 * @return le numéro ACI de la commune.
	 */
	String getNoACI();

	/**
	 * Retourne le numéro d'identification cantonal de la commune.
	 *
	 * @return le numéro d'identification cantonal de la commune.
	 */
	String getNoCantonal();

	/**
	 * Retourne le nom abrégé de la commune.
	 *
	 * @return le nom abrégé de la commune.
	 */
	String getNomAbrege();

	/**
	 * @return le numéro OFS étendu, c'est-à-dire le numéro OFS officiel pour les communes non-fractionnées et un pseudo-numéro OFS cantonal
	 *         (> 8000) pour les fractions de commune.
	 */
	int getNoOFSEtendu();

	/**
	 * Retourne le numéro technique de la commune à laquelle la commune est rattachée.
	 *
	 * @return le numéro technique de la commune à laquelle la commune est rattachée
	 */
	int getNumTechMere();

	/**
	 * Retourne le sigle du canton de la commune.
	 *
	 * @return le sigle du canton de la commune.
	 */
	String getSigleCanton();

	/**
	 * Retourne <code>true</code> si la commune (ou fraction) est vaudoise, <code>false</code> sinon
	 * @return
	 */
	boolean isVaudoise();

	/**
	 * Indique si la commune, dans le cadre d'un regroupement, est une fraction de commune.
	 *
	 * @return <code>true</code> si la commune est une fraction de commune.
	 */
	boolean isFraction();

	/**
	 * Indique si la commune, dans le cadre d'un regroupement, est la commune principale.
	 *
	 * @return <code>true</code> si la commune, dans le cadre d'un regroupement, est la commune principale.
	 */
	boolean isPrincipale();

	/**
	 * Indique si la commune est valide à la date du jour.
	 *
	 * @return <code>true</code> si la date de fin de validité de la commune n'est pas renseignée ou si la date de fin de validité de la
	 *         commune est égale ou postérieure à la date du jour.
	 */
	boolean isValide();
}
