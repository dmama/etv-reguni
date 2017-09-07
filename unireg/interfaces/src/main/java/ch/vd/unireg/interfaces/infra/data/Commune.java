package ch.vd.unireg.interfaces.infra.data;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public interface Commune extends EntiteOFS, DateRange {

	/**
	 * @return la date de début de validité de la commune.
	 */
	RegDate getDateDebutValidite();

	/**
	 * @return la date de fin de validité de la commune.
	 */
	RegDate getDateFinValidite();

	/**
	 * @return le numéro Ofs de la commune faîtière si la commune est une fraction; ou <b>-1</b> si la commune n'est pas une fraction.
	 */
	int getOfsCommuneMere();

	/**
	 * @return le sigle du canton de la commune.
	 */
	String getSigleCanton();

	/**
	 * @return le nom officiel de la commune avec la mention du canton (même pour les communes vaudoises)
	 */
	String getNomOfficielAvecCanton();

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

	/**
	 * @return le code du district de la commune vaudoise; ou <b>null</b> s'il s'agit d'une commune hors-canton.
	 */
	Integer getCodeDistrict();

	/**
	 * @return le code de la région de la commune vaudoise; ou <b>null</b> s'il s'agit d'une commune hors-canton.
	 */
	Integer getCodeRegion();
}
