package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface Commune extends EntiteOFS {

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
	 * @return le numéro OFS étendu, c'est-à-dire le numéro OFS officiel pour les communes non-fractionnées et un pseudo-numéro OFS cantonal
	 *         (> 8000) pour les fractions de commune.
	 */
	int getNoOFSEtendu();

	/**
	 * Retourne le numéro technique de la commune à laquelle la commune est
	 * rattachée.
	 *
	 * @return le numéro technique de la commune à laquelle la commune est
	 *         rattachée
	 */
	int getNumTechMere();

	/**
	 * Retourne le sigle du canton de la commune.
	 *
	 * @return le sigle du canton de la commune.
	 */
	String getSigleCanton();

	/**
	 * @return <code>true</code> si la commune (ou fraction) est vaudoise, <code>false</code> sinon
	 */
	boolean isVaudoise();

	/**
	 * Indique si la commune, dans le cadre d'un regroupement, est une fraction
	 * de commune.
	 *
	 * @return <code>true</code> si la commune est une fraction de commune.
	 */
	boolean isFraction();

	/**
	 * Indique - dans le cadre d'un regroupement - si la commune est la commune principale (faîtière).
	 *
	 * @return <code>true</code> si la commune, dans le cadre d'un regroupement, est la commune principale; <code>false</code> s'il s'agit d'une fraction de commune ou d'une commune non-fractionnée.
	 */
	boolean isPrincipale();
}
