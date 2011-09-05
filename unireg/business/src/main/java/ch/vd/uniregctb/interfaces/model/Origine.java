package ch.vd.uniregctb.interfaces.model;

public interface Origine {

	/**
	 * Retourne le nom du lieu d'origine. Cet attribut est obligatoire pour les citoyens suisses; il est <code>null</code> pour les étrangers.
	 *
	 * @return le nom du lieu (commune, localité, ...) d'origine.
	 */
	String getNomLieu();

	/**
	 * Le sigle du canton d'origine. Cet attribut est obligatoire pour les citoyens suisses; il est <code>null</code> pour les étrangers.
	 *
	 * @return le sigle du canton d'origine.
	 */
	String getSigleCanton();

	/**
	 * Retourne le pays de l'origine. Cette attribut peut être <code>null</code> si l'origine est suisse.
	 *
	 * @return le pays de l'origine.
	 */
	Pays getPays();
}
